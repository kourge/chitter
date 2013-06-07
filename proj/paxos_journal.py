import Journal

class PaxosJournal(Journal):
    def __init__(self, node):
        Journal.__init__(self, "$paxos_journal", node)

    def execute(entry):
        field, value = entry
        setattr(self.node, field, value)
