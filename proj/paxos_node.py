import AbstractNode
import Protocol
import Serialization

from paxos_journal import *
from paxos_message import *
from paxos_roles import *


class PaxosNode(AbstractNode, PaxosAcceptor, PaxosLearner, PaxosProposer):
    def __init__(self):
        PaxosAcceptor.__init__(self)
        PaxosLearner.__init__(self)
        PaxosProposer.__init__(self)

    def start(self):
        self.nodes = {self.addr}
        self.journal = PaxosJournal(self)
        self.broadcast(CATCH_UP(self.learned_seq))

    def onCommand(self, cmd_str):
        tokens = cmd_str.split()
        cmd = tokens[0]

        if cmd == "paxos_setup":
            nodes_copy = self.nodes.copy()
            nodes_copy.update([int(x) for x in tokens[1:]])
            self.nodes = nodes_copy
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
        print "\n\n"
        print "Broadcasting", msg, "to", self.nodes
        print "\n\n"
        for dest_addr in self.nodes:
            self.send_msg(dest_addr, msg, error_str)

    ## general paxos related methods

    def announce(self, src_addr, msg):
        self.nodes.add(src_addr)
