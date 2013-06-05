def PaxosAcceptor(object):
    def __init__(self):
        super(PaxosAcceptor, self).__init__()

    def accept(self, src_addr, msg):
        pass

    def prepare(self, src_addr, msg):
        pass

def PaxosLearner(object):
    def __init__(self):
        super(PaxosLearner, self).__init__()

    def learn(self, src_addr, msg):
        pass

def PaxosProposer(object):
    def __init__(self):
        super(PaxosProposer, self).__init__()

    def accepted(self, src_addr, msg):
        pass

    def promise(self, src_addr, msg):
        pass

    def nack(self, src_addr, msg):
        pass
