import PaxosMessage
import Protocol
import PaxosConfigNode

class PaxosNode(PaxosConfigNode):
    def __init__(self):
        super(self.__class__, self).__init__()

    def start(self):
        pass

    def onRIOReceive(self, from_address, protocol, msg):
        if protocol != Protocol.PAXOS:
            return

        # TODO deserialize msg to a PaxosMessage type
        pass
