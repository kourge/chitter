from java.lang import System, String, Long, StringBuffer
from java.util import Scanner
from edu.washington.cs.cse490h.lib import Utility
import Serialization
import Op
import FS
import Chit
import Pair
from pyfs import Transaction


FAILURE = -1
INVALID_USERNAME_CHARACTERS = ["\n", "\t"]
__ops = []


def signature(*args):
    """Annotates a function with a type signature of sorts."""
    def decorator(f):
        __ops.append(f.__name__)
        f._sig = args
        return f
    return decorator


def valid_username(username):
    return not any(char in INVALID_USERNAME_CHARACTERS for char in username)


def lines(iterable, separator="\n"):
    """A generator that "splits" an iterable by a character separator."""
    if len(iterable) == 0:
        raise StopIteration
    if not isinstance(iterable, basestring):
        separator = ord(separator)

    begin, end = 0, 0
    for i in xrange(len(iterable)):
        if iterable[i] == separator:
            end = i
            window = iterable[begin:end]
            if len(window) != 0:
                yield window
            begin = end + 1
    window = iterable[begin:]
    if len(window) != 0:
        yield window


class RemoteOp(Op):
    def __init__(self):
        pass

    def _parse_args(self, cmd_name, cmd_str):
        """Parses a cmd_str for the generator for cmd_name by reading its
        parsing signature decorated with @signature."""

        args = []
        scanner = Scanner(cmd_str)

        for kind in getattr(self, cmd_name)._sig:
            if kind is str:
                args.append(scanner.next())
            elif kind is Ellipsis:
                args.append(scanner.nextLine())

        return args

    def __call__(self, cmd_name, cmd_str):
        """Parses the arguments in cmd_str and initializes the generator
        corresponding to cmd_name."""

        args = self._parse_args(cmd_name, cmd_str)
        return getattr(self, cmd_name)(*args)

    __docs = None
    @classmethod
    def __docs__(cls):
        """Lazily collects the docstrings of generators decorated with
        @signature into a dictionary and caches the result."""

        if cls.__docs is not None:
            return cls.__docs

        cls.__docs = {f: getattr(cls, f).__doc__ for f in __ops}
        return cls.__docs

    def _content_to_chits(self, content):
        try:
            return [Serialization.decode(line) for line in lines(content)]
        except Serialization.DecodingException as e:
            e.printStackTrace()
            return None

    @signature(str)
    def createUser(self, username):
        """createUser username"""

        if not valid_username(username):
            yield False

        tweets_fn = "tweets:" + username
        following_fn = "following:" + username
        user_fn = "users:" + username

        with Transaction() as fs:
            yield fs.begin()

            if not (yield fs.create(tweets_fn)):
                yield False

            if not (yield fs.create(following_fn)):
                yield False

            if not (yield fs.create(user_fn)):
                yield False

            content = "%d" % (0,)
            if not (yield fs.overwrite(user_fn, Utility.stringToByteArray(content))):
                yield False

            yield (yield fs.commit())

    @signature(str)
    def verify(self, username):
        """verify username"""

        if not valid_username(username):
            yield False

        yield True

    @signature(str, Ellipsis)
    def chit(self, username, text):
        """chit username text"""

        chit = Chit(text)
        content = None

        try:
            content = Serialization.encode(chit)
        except Serialization.EncodingException as e:
            yield False

        content.append(ord("\n"))

        tweets_fn = "tweets:" + username

        with Transaction() as fs:
            yield fs.begin()

            if not (yield fs.append(tweets_fn, content)):
                yield False

            yield (yield fs.commit())

    @signature(str, str)
    def addFollower(self, username, follower):
        """addFollower username follower"""

        following_fn = "following:" + follower

        with Transaction() as fs:
            yield fs.begin()

            content = (yield fs.read(following_fn))
            if content is None:
                yield Op.FollowerChangeResult.FAILURE

            content = Utility.byteArrayToString(content)

            for line in lines(content):
                followed_user, timestamp = line.split("\t")
                if followed_user == username:
                    yield Op.FollowerChangeResult.ALREADY_EXISTS

            line = "%s\t%d\n" % (username, System.currentTimeMillis())

            if not (yield fs.append(following_fn, Utility.stringToByteArray(line))):
                yield Op.FollowerChangeResult.FAILURE

            if not (yield fs.commit()):
                yield Op.FollowerChangeResult.FAILURE

            yield Op.FollowerChangeResult.SUCCESS

    @signature(str, str)
    def removeFollower(self, username, follower):
        """removeFollower username follower"""

        following_fn = "following:" + follower

        with Transaction() as fs:
            yield fs.begin()

            content = (yield fs.read(following_fn))
            if content is None:
                yield Op.FollowerChangeResult.FAILURE

            content = Utility.byteArrayToString(content)
            out = StringBuffer()
            absent = True

            for line in lines(content):
                followed_user, timestamp = line.split("\t")
                if followed_user == username:
                    absent = False
                else:
                    out.append(line + "\n")

            fs.overwrite(following_fn, Utility.stringToByteArray(out.toString()))
            if not (yield fs.commit()):
                yield Op.FollowerChangeResult.FAILURE
            elif absent:
                yield Op.FollowerChangeResult.DOES_NOT_EXIST
            else:
                yield Op.FollowerChangeResult.SUCCESS

    @signature(str)
    def getChits(self, username):
        """getChits username"""

        tweets_fn = "tweets:" + username

        with Transaction() as fs:
            yield fs.begin()

            content = (yield fs.read(tweets_fn))
            if content is None:
                yield None

            yield fs.commit()
            yield self._content_to_chits(content)

    @signature(str)
    def getTimeline(self, username):
        """getTimeline username"""

        result = []
        user_fn = "users:" + username

        with Transaction() as fs:
            yield fs.begin()

            content = (yield fs.read(user_fn))
            if content is None:
                yield None

            content = Utility.byteArrayToString(content)
            timestamp = long(content)

            following_fn = "following:" + username
            followings = []

            content = (yield fs.read(following_fn))
            if content is None:
                yield None

            for line in lines(Utility.byteArrayToString(content)):
                followed, follow_timestamp = line.split("\t")
                followings.append(Pair(followed, Long(follow_timestamp)))

            results = []
            for user_followed, follow_timestamp in followings:
                tweets = (yield fs.read("tweets:" + user_followed))
                if tweets is None:
                    yield None
                else:
                    results.append(tweets)

            for content in results:
                chits = self._content_to_chits(content)
                if chits is not None:
                    cutoff = max(follow_timestamp, timestamp)
                    result.extend(chit for chit in chits if chit.timestamp > cutoff)

            timestamp = System.currentTimeMillis()
            payload = Utility.stringToByteArray(str(timestamp))

            if not (yield fs.overwrite(user_fn, payload)):
                yield None

            if not (yield fs.commit()):
                yield None

        result.sort()
        yield result
