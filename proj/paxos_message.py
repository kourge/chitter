__doc__ = """
The paxos_message module exports sugared constructor functions for PaxosMessages
of different types.

from paxos_message import *
LEARN(seq, value) == PaxosMessage(Kind.LEARN, (seq, value))
"""

def enum(**enums):
    return type('Enum', (), enums)

TYPES = (
    'ACCEPT',
    'ACCEPTED',
    'CATCH_UP',
    'LEARN',
    'NACK',
    'PREPARE',
    'PROMISE',
    'UPDATE',
)

class PaxosMessage:
    Kind = enum(**{name: name for name in TYPES})

    def __init__(self, kind=None, value=None):
        self.kind = kind
        self.data = value

    def __repr__(self):
        return "%s%r" % (self.kind, self.data)

# Map each kind to a function that constructs a corresponding PaxosMessage.
for kind in TYPES:
    globals()[kind] = (lambda name: lambda *args: PaxosMessage(name, args))(kind)

__all__ = ['PaxosMessage']
__all__.extend(TYPES)
