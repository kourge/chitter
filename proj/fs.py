from java.lang import Object
from array import array
import Invocation
import FS

class MockFS(object):
    def __init__(self):
        pass

    def __getattr__(self, name):
        def wrapper(*args):
            return (name, args)

        wrapper.__name__ = name
        return wrapper


