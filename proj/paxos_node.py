import Protocol
import PaxosConfigNode
import Serialization

from paxos_roles import *


def enum(**enums):
    return type('Enum', (), enums)


class PaxosMessage:
    Kind = enum(
        ACCEPT="accept",
        ACCEPTED="accepted",
        ANNOUNCE="announce",
        NACK="nack",
        PREPARE="prepare",
        PROMISE="promise"
    )

    def __init__(self, kind=None, seq=None, value=None):
        self.kind = kind
        self.seq = seq
        self.data = value

    def __repr__(self):
        contents = ["kind:", self.kind, "seq:", self.seq, "data:", self.data]
        return " ".join([str(x) for x in contents])


class PaxosNode(PaxosConfigNode, PaxosAcceptor, PaxosLearner, PaxosProposer):
    def __init__(self):
        super(PaxosNode, self).__init__()
        self.states = enum(ACCEPTOR="acceptor", LEARNER="learner", PROPOSER="proposer")

    def start(self):
        self.nodes = {self.addr}
        self.leader = self.addr
        self.last_seq = None
        self.last_value = None

    def onCommand(self, cmd_str):
        tokens = cmd_str.split()
        cmd = tokens[0]

        if cmd == "paxos_setup":
            self.nodes.extend([int(x) for x in tokens[1:]])
        elif cmd == "paxos_propose":
            self.propose(tokens[1])

    def onRIOReceive(self, src_addr, protocol, byte_msg):
        if protocol != Protocol.PAXOS:
            return

        try:
            msg = Serialization.decode(byte_msg)
        except Serialization.DecodingException as e:
            # TODO(sumanvyj): better logging
            print "Failed to decode RPC reply."
            return

        try:
            getattr(self, msg.kind)(src_addr, msg)
        except AttributeError:
            print "Invalid paxos message kind"
            return

    def send_msg(self, dest_addr, msg, error_str="Message sending failed"):
        try:
            byte_msg = Serialization.encode(msg)
        except Serialization.EncodingException as e:
            print error_str
            return

        self.RIOSend(dest_addr, Protocol.PAXOS, byte_msg)

    def propose(self, value):
        msg = PaxosMessage()
        msg.kind = PaxosMessage.Kind.PREPARE

        if self.last_seq:
            msg.seq = self.last_seq + len(self.nodes)
            msg.data = value
        else:
            msg.seq = self.addr

        for dest_addr in self.nodes:
            self.send_msg(dest_addr, msg, "Proposal failed")

    ## general paxos related methods

    def announce(self, src_addr, msg):
        self.nodes.add(src_addr)
