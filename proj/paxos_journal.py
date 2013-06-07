import Journal

class PaxosJournal(Journal):
    def __init__(self, node):
        fn = "$paxos_journal_%d" % (node.addr,)
        Journal.__init__(self, fn, node)

    def execute(entry):
        field, value = entry
        setattr(self.node, field, value)
