def enum(**enums):
    return type('Enum', (), enums)

TYPES = (
    'ACCEPT', 'ACCEPTED', 'ANNOUNCE', 'LEARN', 'NACK', 'PREPARE', 'PROMISE'
)

class PaxosMessage:
    Kind = enum(**{name: name for name in TYPES})

    def __init__(self, kind=None, value=None):
        self.kind = kind
        self.data = value

    def __repr__(self):
        return "%s(kind=%r, data=%r)" % (
            self.__class__.__name__, self.kind, self.data
        )

# Map each kind to a function that constructs a corresponding PaxosMessage.
for kind in TYPES:
    globals()[kind] = (lambda name: lambda *args: PaxosMessage(name, args))(kind.lower())

__all__ = ['PaxosMessage']
__all__.extend(TYPES)
