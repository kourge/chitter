from java.lang import String

class MockFS(object):
    def __init__(self):
        pass

    def __getattr__(self, name):
        def wrapper(*args):
            params = []
            for arg in args:
                if isinstance(arg, basestring):
                    arg = String(arg)
                params.append(arg)
            return (name, tuple(params))

        wrapper.__name__ = name
        return wrapper


