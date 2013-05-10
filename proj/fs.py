import Invocation
import FS

class MockFS(object):
    def __init__(self):
        pass

    def __getattr__(self, name):
        def wrapper(*args):
            iv = Invocation.of(FS, name)
            types = iv.getParameterTypes()
            parameters = args.toArray()

            # Coerce objects in array to Java equivalents
            for i in xrange(len(parameters)):
                parameters[i] = types[i](parameters[i])

            return iv

        wrapper.__name__ = name
        return wrapper


