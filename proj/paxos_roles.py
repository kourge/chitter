from paxos_message import *


class PaxosAcceptor(object):
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


class PaxosLearner(object):
    def __init__(self):
        pass

    def learn(self, src_addr, msg):
        # TODO(sumanvyj): save to disk/MemoryFS
        # TODO(sumanvyj): ignore duplicate learnings
        print self.addr, "learned", msg.data


class PaxosProposer(object):
    def __init__(self):
        self.promises = 0
        self.proposed_seq = 0
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
        if max_used:
            seq = max_used - (max_used % len(self.nodes)) + self.addr
            if seq <= max_used:
                seq += node_num
        else:
            seq = self.addr
        return seq

    def propose(self, value):
        self.promises = 0
        self.proposed_value = value
        self.broadcast(PREPARE(self.next_seq()), "Proposal failed")

    def accepted(self, src_addr, msg):
        # TODO(kourge): respond to user somehow

        # updated accepted seq and value
        self.accepted_seq, self.accepted_value = msg.data

    def promise(self, src_addr, msg):
        seq, value = msg.data

        # ensure that the promiser and the current node are on the same page
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
            # we need to catch up
            pass
        else: # seq > self.accepted_seq
            # the acceptor needs to catch up
            pass

    def nack(self, src_addr, msg):
        # proposal rejected, re-propose with higher seq
        self.propose(self.proposed_value)
