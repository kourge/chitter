from java.lang import String


class RPC(object):
    def __init__(self, value):
        self.value = value

    def __call__(self):
        return self.value


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
