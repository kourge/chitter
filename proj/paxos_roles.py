from paxos_message import *

def log(*args):
    print "[PAXOS]", " ".join([str(x) for x in list(args)])

class PaxosRole(object):
    logged_names = [
        "nodes",
        "promised_seq",
        "accepted_seq",
        "accepted_value",
        "learned_seq",
        "learned_value",
        "learned",
        "original_proposed_value",
        "promises",
        "proposed_seq",
        "proposed_value",
    ]

    def __setattr__(self, name, value):
        try:
            if name in PaxosRole.logged_names:
                self.journal.push((name, value))
        except AttributeError:
            pass
        super(PaxosRole, self).__setattr__(name, value)

class PaxosAcceptor(PaxosRole):
    def __init__(self):
        # last promised sequence
        self.promised_seq = None
        
        # last accepted seq and value
        self.accepted_seq = None
        self.accepted_value = None

    def accept(self, src_addr, msg):
        seq, value = msg.data

        # Accept the proposal if the sequence number is greater than or equal to
        # what we had last promised; if this is not the case, simply ignore the
        # proposer.
        if seq >= self.promised_seq:
            self.accepted_seq = seq
            self.accepted_value = value

            # Tell the original proposer that the proposal has been accepted and
            # instruct all learners to learn the value.
            self.send_msg(src_addr, ACCEPTED(seq, value))
            self.broadcast(LEARN(seq, value))

            # If this acceptor was also the proposer, then directly call
            # self.on_paxos_result (the proposer won't do it since
            # self.accepted_seq and self.accepted_value will already have been
            # updated here)
            if self.accepted_seq == self.proposed_seq:
                self.on_paxos_consensus(
                    value == self.original_proposed_value,
                    self.original_proposed_value
                )

    def prepare(self, src_addr, msg):
        seq, = msg.data

        # Send a promise if the sequence number is greater than or equal to what
        # we had last promised. Otherwise, nack the proposal.
        if seq >= self.promised_seq:
            self.promised_seq = seq
            new_msg = PROMISE(self.accepted_seq, self.accepted_value)
        else:
            new_msg = NACK(seq)
        self.send_msg(src_addr, new_msg)


class PaxosLearner(PaxosRole):
    def __init__(self):
        self.learned_seq = None
        self.learned_value = None
        self.learned = {}

    def learn(self, src_addr, msg):
        seq, value = msg.data

        if seq > self.learned_seq:
            self.learned_seq, self.learned_value = seq, value
            self.learned[seq] = value

            log(self.addr, "LEARNED", value, "with seq", seq)

    def catch_up(self, src_addr, msg):
        seq, = msg.data
        updates = {k:self.learned[k] for k in self.learned if k > seq} 
        self.send_msg(src_addr, UPDATE(self.learned_seq, self.learned_value, updates))

    def update(self, src_addr, msg):
        self.learned_seq, self.learned_value, updates = msg.data
        self.learned.update(updates)
        self.learned = self.learned


class PaxosProposer(PaxosRole):
    def __init__(self):
        self.promises = 0
        self.proposed_seq = None
        self.original_proposed_value = None
        self.proposed_value = None

    # The sequence numbers allowed for a given node are of the form
    # len(self.nodes) * x + i, where x is some multiplier and i is the node's
    # address.
    #
    # Given a previously accepted or proposed sequence number, find the
    # corresponding len(self.nodes) * x, add the current node's address, and
    # then increment by len(self.nodes) if necessary to make the seq greater
    # than the previously accepted seq.
    def next_seq(self):
        max_used = max(self.accepted_seq, self.proposed_seq)
        if max_used is not None:
            seq = max_used - (max_used % len(self.nodes)) + self.addr
            if seq <= max_used:
                seq += len(self.nodes)
        else:
            seq = self.addr
        return seq

    def propose(self, value):
        self.promises = 0
        self.proposed_seq = self.next_seq()
        self.original_proposed_value = value
        self.proposed_value = value

        log(self.addr, "PROPOSING", self.proposed_value, "with seq", self.proposed_seq)

        self.broadcast(PREPARE(self.proposed_seq), "Proposal failed")

    def accepted(self, src_addr, msg):
        # update accepted seq and value
        seq, value = msg.data
        if self.accepted_seq < seq:
            self.accepted_seq, self.accepted_value = msg.data
            self.on_paxos_consensus(
                value == self.original_proposed_value,
                self.original_proposed_value
            )

    def promise(self, src_addr, msg):
        seq, value = msg.data

        # ensure that the promiser and the proposer are on the same page
        if seq == self.accepted_seq:
            self.promises += 1

            # set the value of node's proposal to the value associated with the
            # highest proposal number reported by any acceptor
            if seq > self.proposed_seq:
                self.proposed_seq = seq
                self.proposed_value = value

            # quorum has been reached, broadcast ACCEPT
            if self.promises > len(self.nodes) / 2:
                self.broadcast(ACCEPT(self.proposed_seq, self.proposed_value))
        elif seq < self.accepted_seq:
            # the acceptor needs to catch up
            updates = {k:self.learned[k] for k in self.learned if k > seq} 
            self.send_msg(src_addr, UPDATE(self.learned_seq, self.learned_value, updates))
            self.send_msg(src_addr, PREPARE(self.proposed_seq), "Proposal failed")
        else: # seq > self.accepted_seq
            # we need to catch up
            self.send_msg(src_addr, CATCH_UP(self.accepted_seq))

    def nack(self, src_addr, msg):
        # proposal rejected, forward to client
        self.on_paxos_consensus(False, self.original_proposed_value)
