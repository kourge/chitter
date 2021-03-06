import AbstractNode
import Protocol
import Serialization

from paxos_journal import *
from paxos_message import *
from paxos_roles import *


class PaxosNode(AbstractNode, PaxosAcceptor, PaxosLearner, PaxosProposer):
    """A PaxosNode inherits from an AbstractNode, which itself is a Java class
    that subclasses RIONode. PaxosNode also subclasses three PaxosRoles:
    PaxosAcceptor, PaxosLearner, and PaxosProposer, all of which act as mixins.
    This ensures a virtual separation between different Paxos roles, but still
    allows all these behaviors on a single node.
    """
    def __init__(self):
        PaxosAcceptor.__init__(self)
        PaxosLearner.__init__(self)
        PaxosProposer.__init__(self)

    def start(self):
        self.nodes = {self.addr}
        self.paxos_journal = PaxosJournal(self)
        self.broadcast(CATCH_UP(self.learned_seq))

    def onCommand(self, cmd_str):
        tokens = cmd_str.split()
        cmd = tokens[0]

        if cmd == "paxos_setup":
            self.nodes.update([int(x) for x in tokens[1:]])
            self.nodes = self.nodes
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
            handler = getattr(self, msg.kind.lower())
            handler(src_addr, msg)
        except AttributeError:
            print "Invalid paxos message kind:", msg.kind
            return

    def send_msg(self, dest_addr, msg, error_str="Message sending failed"):
        try:
            byte_msg = Serialization.encode(msg)
        except Serialization.EncodingException as e:
            print error_str
            return

        self.RIOSend(dest_addr, Protocol.PAXOS, byte_msg)

    def broadcast(self, msg, error_str="Message broadcasting failed"):
        log(self.addr, "Broadcasting", msg, "to nodes", self.nodes)

        for dest_addr in self.nodes:
            self.send_msg(dest_addr, msg, error_str)

    ## general paxos related methods

    def announce(self, src_addr, msg):
        self.nodes.add(src_addr)

    def on_paxos_consensus(self, consensus_reached, value):
        log(self.addr, "on_paxos_consensus", consensus_reached, value)

        raise NotImplementedError
