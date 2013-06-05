# An RPC object allows a caller to distinguish between a genuine return value
# from a generator and a request for an RPC to be performed. This is needed
# because Python generators are not allowed to return, only yield.
class RPC(object):
    def __init__(self, value):
        self.value = value

    def __call__(self):
        return self.value


# A Transaction works as a context manager. Within the context, any non-special
# method call on the transaction returns a tuple (name, args) wrapped in an RPC
# object, where name is a string that is the method name called, and args is a
# tuple of the arguments supplied.
#
# The two special methods are begin() and commit(). Yielding them in a generator
# tells the caller to either begin a new transaction or commit the current one.
# The result of yielding commit() is True if the transaction was committed
# successfully, or False if it failed.
#
# This allows a generator to write something like this:
#     with Transaction() as t:
#         yield t.begin()
#         yield t.create(filename)
#         yield t.overwrite(filename, array('b', [108, 111, 108]))
#         content = (yield t.read(filename))
#         if not (yield t.commit()):
#             yield False
#     yield True
class Transaction(object):
    Commit = object()

    def __getattr__(self, name):
        def wrapper(*args):
            return RPC((name, args))

        wrapper.__name__ = name
        return wrapper

    def begin(self):
        return self

    def commit(self):
        return self.Commit

    def __enter__(self):
        return self

    def __exit__(self, exception_type, exception_value, traceback):
        pass
