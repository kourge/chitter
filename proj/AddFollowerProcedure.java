import java.util.Scanner;

public class AddFollowerProcedure extends ChitterProcedure {
    private AddFollowerProcedure() {}

    @Proc(name="addFollower", desc="addFollower username follower")
    public static AddFollowerProcedure make(
        ClientServerNode node, Invocation onComplete, int destination,
        String commandString
    ) {
        Scanner scanner = new Scanner(commandString);
        String username = scanner.next();
        String follower = scanner.next();

        return new AddFollowerProcedure(
            node, onComplete, destination, username, follower
        );
    }

    private String username;
    private String followingFn;

    public AddFollowerProcedure(
        ClientServerNode node, Invocation onComplete, int destination,
        String username, String follower
    ) {
        populate(node, onComplete, destination);

        checkUsername(username);
        checkUsername(follower);

        this.username = username;
        followingFn = "following:" + follower;
    }

    public void call() throws Exception {
        doThen(
            Invocation.of(fs, "read", followingFn),
            Invocation.on(this, "setResult")
        );
    }

    private Pair<byte[], Long> result;
    @SuppressWarnings("unchecked")
    public void setResult(Object obj) throws Exception {
        result = (Pair<byte[], Long>)obj;

        if (result == null) {
            returnValue(Op.FollowerChangeResult.FAILURE);
            return;
        }
        String lines = new String(result.first());
        long version = result.second();

        Scanner scanner = new Scanner(lines);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            int delimiter = line.indexOf("\t");
            String followedUser = line.substring(0, delimiter);

            if (followedUser.equals(username)) {
                returnValue(Op.FollowerChangeResult.ALREADY_EXISTS);
                return;
            }
        }

        String line = String.format(
            "%s\t%d\n", username, System.currentTimeMillis()
        );

        doThen(
            Invocation.of(
                fs, "appendIfNotChanged",
                followingFn, line.getBytes(), version
            ),
            Invocation.on(this, "setFollowingV")
        );
    }

    private long followingV;
    public void setFollowingV(Object obj) throws Exception {
        followingV = (Long)obj;
        if (followingV == FAILURE) {
            returnValue(Op.FollowerChangeResult.FAILURE);
            return;
        }

        returnValue(Op.FollowerChangeResult.SUCCESS);
        return;
    }
}
