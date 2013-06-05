import PaxosMessage
import Protocol
import RIONode

class PaxosNode(RIONode):
    def __init__(self):
        super(self.__class__, self).__init__()

    @staticmethod
    def getFailureRate(): return 0.0

    @staticmethod
    def getRecoveryRate(): return 1.0

    @staticmethod
    def getDropRate(): return 0.0

    @staticmethod
    def getDelayRate(): return 0.0

    def onRIOReceive(self, from_address, protocol, msg):
        if protocol != Protocol.PAXOS:
            return

        # TODO deserialize msg to a PaxosMessage type
        pass
