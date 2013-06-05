import Protocol
import PaxosConfigNode
import Serialization


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


# TODO(sumanvy): create separate Acceptor, Learner, and Proposer classes that
# can be mixed in
class PaxosNode(PaxosConfigNode):
    def __init__(self):
        super(self.__class__, self).__init__()
        self.states = enum(ACCEPTOR="acceptor", LEARNER="learner", PROPOSER="proposer")

    def start(self):
        self.nodes = [self.addr]
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

    def onRIOReceive(self, from_address, protocol, byte_msg):
        if protocol != Protocol.PAXOS:
            return

        try:
            msg = Serialization.decode(byte_msg)
        except Serialization.DecodingException as e:
            # TODO(sumanvyj): better logging
            print "Failed to decode RPC reply."
            return

        try:
            getattr(self, msg.kind)(from_address, msg)
        except AttributeError:
            print "Invalid paxos message kind"
            return

    def send_msg(self, dest_addr, byte_msg):
        self.RIOSend(dest_addr, Protocol.PAXOS, byte_msg)

    def propose(self, value):
        msg = PaxosMessage()
        msg.kind = PaxosMessage.Kind.PREPARE

        if self.last_seq:
            msg.seq = self.last_seq + len(self.nodes)
            msg.data = value
        else:
            msg.seq = self.addr

        try:
            byte_msg = Serialization.encode(msg)
        except Serialization.EncodingException as e:
            print "Proposal failed"
            return

        for dest_addr in self.nodes:
            self.send_msg(dest_addr, byte_msg)

    ## paxos related methods

    def accept(self, from_address, msg):
        pass

    def accepted(self, from_address, msg):
        pass

    def announce(self, from_address, msg):
        if from_address not in self.nodes:
            self.nodes.append(from_address)

    def nack(self, from_address, msg):
        pass

    def prepare(self, from_address, msg):
        pass

    def promise(self, from_address, msg):
        pass
