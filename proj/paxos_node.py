import Protocol
import PaxosConfigNode
import Serialization

from paxos_roles import *
from paxos_message import *


class PaxosNode(PaxosConfigNode, PaxosAcceptor, PaxosLearner, PaxosProposer):
    def __init__(self):
        PaxosAcceptor.__init__(self)
        PaxosLearner.__init__(self)
        PaxosProposer.__init__(self)

    def start(self):
        self.nodes = {self.addr}
        # TODO(sumanvyj): recover from log and/or catch-up to other nodes if
        # necessary

    def onCommand(self, cmd_str):
        tokens = cmd_str.split()
        cmd = tokens[0]

        if cmd == "paxos_setup":
            self.nodes.update([int(x) for x in tokens[1:]])
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
        for dest_addr in self.nodes:
            self.send_msg(dest_addr, msg, error_str)

    ## general paxos related methods

    def announce(self, src_addr, msg):
        self.nodes.add(src_addr)
