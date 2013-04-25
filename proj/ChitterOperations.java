import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;

public class ChitterOperations {
    public enum FollowerChangeResult {
        SUCCESS, FAILURE, ALREADY_EXISTS, DOES_NOT_EXIST
    };

    private FSCommands fs;
    private static final long FAILURE = -1;
    private static final char[] INVALID_USERNAME_CHARACTERS = { '\t', '\n' };

    public ChitterOperations(FSCommands fs) {
        this.fs = fs;
    }

    public boolean createUser(String username) {
        for (char invalidCharacter : INVALID_USERNAME_CHARACTERS) {
            if (username.indexOf(invalidCharacter) != -1) {
                return false;
            }
        }

        String tweetsFn = "tweets:" + username;
        String followingFn = "following:" + username;
        String userFn = "users:" + username;

        long tweetsV = fs.create(tweetsFn);
        if (tweetsV == FAILURE) {
            return false;
        }

        long followingV = fs.create(followingFn);
        if (followingV == FAILURE) {
            return false;
        }

        long userV = fs.create(userFn);
        if (userV == FAILURE) {
            return false;
        }

        String content = String.format("%d", 0);

        long version = fs.currentVersion(userFn);
        userV = fs.overwriteIfNotChanged(userFn, content.getBytes(), version);
        if (userV == FAILURE) {
            return false;
        }

        return true;
    }

    public boolean verify(String username) {
        return true;
    }

    public boolean chit(String username, String text) {
        Chit chit = new Chit(text);
        byte[] stream;

        try {
            stream = Serialization.encode(chit);
        } catch (Serialization.EncodingException e) {
            return false;
        }

        String line = new String(stream) + "\n";

        String tweetsFn = "tweets:" + username;
        long version = fs.currentVersion(tweetsFn);

        long tweetsV = fs.appendIfNotChanged(tweetsFn, line.getBytes(), version);
        if (tweetsV == FAILURE) {
            return false;
        }

        return true;
    }

    public FollowerChangeResult addFollower(String username, String follower) {
        String followingFn = "following:" + follower;

        Pair<byte[], Long> result = fs.read(followingFn);
        if (result == null) {
            return FollowerChangeResult.FAILURE;
        }
        String lines = new String(result.first());
        long version = result.second();

        Scanner scanner = new Scanner(lines);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            int delimiter = line.indexOf("\t");
            String followedUser = line.substring(0, delimiter);

            if (followedUser.equals(username)) {
                return FollowerChangeResult.ALREADY_EXISTS;
            }
        }

        String line = String.format(
            "%s\t%d\n", username, System.currentTimeMillis()
        );
        long followingV = fs.appendIfNotChanged(
            followingFn, line.getBytes(), version
        );
        if (followingV == FAILURE) {
            return FollowerChangeResult.FAILURE;
        }

        return FollowerChangeResult.SUCCESS;
    }

    public FollowerChangeResult removeFollower(String username, String follower) {
        String followingFn = "following:" + follower;

        Pair<byte[], Long> result = fs.read(followingFn);
        if (result == null) {
            return FollowerChangeResult.FAILURE;
        }
        String lines = new String(result.first());
        long version = result.second();
        StringBuffer out = new StringBuffer();
        boolean absent = true;

        Scanner scanner = new Scanner(lines);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            int delimiter = line.indexOf("\t");
            String followedUser = line.substring(0, delimiter);

            if (followedUser.equals(username)) {
                absent = false;
            } else {
                out.append(line + "\n");
            }
        }

        if (absent) {
            return FollowerChangeResult.DOES_NOT_EXIST;
        }

        long followingV = fs.overwriteIfNotChanged(
            followingFn, out.toString().getBytes(), version
        );
        if (followingV == FAILURE) {
            return FollowerChangeResult.FAILURE;
        }

        return FollowerChangeResult.SUCCESS;
    }

    public List<Chit> getChits(String username) {
        List<Chit> list = new LinkedList<Chit>();

        String tweetsFn = "tweets:" + username;
        Pair<byte[], Long> result = fs.read(tweetsFn);
        if (result == null) {
            return null;
        }
        String lines = new String(result.first());

        Scanner scanner = new Scanner(lines);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Chit chit;
            try {
                chit = (Chit)Serialization.decode(line.getBytes());
            } catch (Serialization.DecodingException e) {
                return null;
            }
            list.add(chit);
        }

        return list;
    }

    public List<Chit> getChitsSince(String username, long timestamp) {
        List<Chit> list = getChits(username);
        List<Chit> result = new LinkedList<Chit>();

        for (Chit chit : list) {
            if (chit.getTimestamp() > timestamp) {
                result.add(chit);
            }
        }

        return result;
    }
}
