from java.lang import String


# An RPC object allows the Java caller to distinguish between a genuine return
# value from a generator and a request for an RPC to be performed. This is
# needed because Python generators are not allowed to return, only yield.
class RPC(object):
    def __init__(self, value):
        self.value = value

    def __call__(self):
        return self.value


# MockFS allows you to call any non-existing method on it. Doing so will give
# a tuple (name, args) wrapped in an RPC object, where name is a string that
# is the method name called, and args is a tuple of the arguments supplied.
# Any Python string-like argument is casted to a Java String.
#
# This allows a generator to write something like this:
#     version = yield self.fs.create(filename)
# Barring the keyword yield, this almost looks like a function call.
class MockFS(object):
    def __init__(self):
        pass

    def __getattr__(self, name):
        def wrapper(*args):
            return RPC((name, tuple(
                (String(x) if isinstance(x, basestring) else x for x in args)
            )))

        wrapper.__name__ = name
        return wrapper


# Transaction does roughly the same thing as MockFS, except for every non-
# existing method called on it, it remembers the method call in a list. When
# this transaction is to be commited, the result of calling this transaction
# should be yielded. This result is the aforementioned list wrapped in an RPC
# object.
#
# This allows a generator to write something like this:
#     with Transaction() as t:
#         t.create(filename)
#         t.overwriteIfNotChanged(filename, array('b', [108, 111, 108]), -1)
#         yield t()
#         if t.failed:
#             yield False
#     yield True
class Transaction(MockFS):
    def __init__(self):
        super(self.__class__, self).__init__()
        self.operations = []

    def __getattr__(self, name):
        wrapper = super(self.__class__, self).__getattr__(name)
        def new_wrapper(*args):
            self.operations.append(wrapper(*args)())

        new_wrapper.__name__ = wrapper.__name__
        return new_wrapper

    def __call__(self):
        return RPC(self.operations)

    def __enter__(self):
        return self

    def __exit__(self, exception_type, exception_value, traceback):
        pass
