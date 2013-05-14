from java.lang import System, String, Long, StringBuffer
from java.util import Scanner
from edu.washington.cs.cse490h.lib import Utility
import Serialization
import Op
import FS
import Chit
import Pair
from pyfs import MockFS


FAILURE = -1
INVALID_USERNAME_CHARACTERS = ["\n", "\t"]


def valid_username(username):
    return not any(char in INVALID_USERNAME_CHARACTERS for char in username)


def lines(iterable, separator="\n"):
    """A generator that 'splits' an iterable by a character separator."""
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
    def __init__(self, fs):
        self.fs = fs

    __signatures = {
        "createUser": [str],
        "verify": [str],
        "chit": [str, Ellipsis],
        "addFollower": [str, str],
        "removeFollower": [str, str],
        "getChits": [str],
        "getFollowings": [str],
        "getTimeline": [str]
    }

    __docs = None

    def _parse_args(self, cmd_name, cmd_str):
        args = []
        scanner = Scanner(cmd_str)

        for kind in self.__signatures[cmd_name]:
            if kind is str:
                args.append(scanner.next())
            elif kind is Ellipsis:
                args.append(scanner.nextLine())

        return args

    def __call__(self, cmd_name, cmd_str):
        args = self._parse_args(cmd_name, cmd_str)
        return getattr(self, cmd_name)(*args)

    @classmethod
    def __docs__(cls):
        if cls.__docs is not None:
            return cls.__docs

        cls.__docs = {x: getattr(cls, x).__doc__ for x in cls.__signatures}
        return cls.__docs

    def createUser(self, username):
        """createUser username"""

        if not valid_username(username):
            yield False

        tweets_fn = String("tweets:" + username)
        following_fn = String("following:" + username)
        user_fn = String("users:" + username)

        tweets_v = yield self.fs.create(tweets_fn)
        if tweets_v == FAILURE:
            yield False

        following_v = yield self.fs.create(following_fn)
        if following_v == FAILURE:
            yield False

        user_v = yield self.fs.create(user_fn)
        if user_v == FAILURE:
            yield False

        content = String("%d" % (0,))

        version = yield self.fs.currentVersion(user_fn)
        user_v = yield self.fs.overwriteIfNotChanged(
            user_fn, Utility.stringToByteArray(content), version
        )
        if user_v == FAILURE:
            yield False

        yield True

    def verify(self, username):
        """verify username"""

        if not valid_username(username):
            yield False

        yield True

    def chit(self, username, text):
        """chit username text"""

        chit = Chit(text)
        content = None

        try:
            content = Serialization.encode(chit)
        except Serialization.EncodingException as e:
            yield False

        content.append(ord("\n"))

        tweets_fn = String("tweets:" + username)
        version = yield self.fs.currentVersion(tweets_fn)

        tweets_v = yield self.fs.appendIfNotChanged(
            tweets_fn, content, version
        )
        if tweets_v == FAILURE:
            yield False

        yield True

    def addFollower(self, username, follower):
        """addFollower username follower"""

        following_fn = String("following:" + follower)

        content, version = yield self.fs.read(following_fn)
        if version == FAILURE:
            yield Op.FollowerChangeResult.FAILURE

        content = Utility.byteArrayToString(content)

        for line in lines(content):
            followed_user, timestamp = line.split("\t")
            if followed_user == username:
                yield Op.FollowerChangeResult.ALREADY_EXISTS

        line = "%s\t%d\n" % (username, System.currentTimeMillis())
        following_v = yield self.fs.appendIfNotChanged(
            following_fn, Utility.stringToByteArray(line), version
        )
        if following_v == FAILURE:
            yield Op.FollowerChangeResult.FAILURE

        yield Op.FollowerChangeResult.SUCCESS

    def removeFollower(self, username, follower):
        """removeFollower username follower"""
        following_fn = String("following:" + follower)

        content, version = yield self.fs.read(following_fn)
        if version == FAILURE:
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

        if absent:
            yield Op.FollowerChangeResult.DOES_NOT_EXIST

        following_v = yield self.fs.overwriteIfNotChanged(
            following_fn, Utility.stringToByteArray(out.toString()), version
        )
        if following_v == FAILURE:
            yield Op.FollowerChangeResult.FAILURE

        yield Op.FollowerChangeResult.SUCCESS

    def getChits(self, username):
        """getChits username"""

        result = []

        tweets_fn = String("tweets:" + username)
        content, version = yield self.fs.read(tweets_fn)
        if version == FAILURE:
            yield None

        for line in lines(content):
            try:
                chit = Serialization.decode(line)
                result.append(chit)
            except Serialization.DecodingException as e:
                e.printStackTrace()
                yield None

        yield result

    def getFollowings(self, username):
        """getFollowings username"""

        following_fn = String("following:" + username)
        result = []

        content, version = yield self.fs.read(following_fn)
        if version == FAILURE:
            yield None

        for line in lines(str(Utility.byteArrayToString(content))):
            followed, timestamp = line.split("\t")
            result.append(Pair(followed, Long(timestamp)))

        yield result

    def getTimeline(self, username):
        """getTimeline username"""

        result = []

        user_fn = String("users:" + username)
        content, version = yield self.fs.read(user_fn)
        if version == FAILURE:
            yield None

        content = Utility.byteArrayToString(content)
        timestamp = long(content)

        proc = self.getFollowings(username)
        followings = proc.send((yield proc.next()))
        for user_followed, follow_timestamp in followings:
            proc = self.getChits(user_followed)
            chits = proc.send((yield proc.next()))

            if chits is not None:
                cutoff = max(follow_timestamp, timestamp)
                result.extend(chit for chit in chits if chit.timestamp > cutoff)

        timestamp = System.currentTimeMillis()
        payload = Utility.stringToByteArray(str(timestamp))
        version = yield self.fs.overwriteIfNotChanged(user_fn, payload, -1)
        if version == -1:
            pass

        result.sort()
        yield result

