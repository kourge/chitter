import edu.washington.cs.cse490h.lib.Node;

import java.util.Scanner;

public class AddFollowerProcedure extends ChitterProcedure {
    private AddFollowerProcedure() {}

    private String username;
    private String followingFn;

    public AddFollowerProcedure(
        ClientServerNode node, Invocation onComplete,
        String username, String follower
    ) {
        populate(node, onComplete);

        checkUsername(username);
        checkUsername(follower);

        this.username = username;
        followingFn = "following:" + follower;
    }

    public void call() throws Exception {
        doThen(
            Invocation.of(FSOperations.class, "read", followingFn),
            Invocation.on(this, "setResult")
        );
    }
    
    private Pair<byte[], Long> result;
    @SuppressWarnings("unchecked")
    public void setResult(Object obj) throws Exception {
        result = (Pair<byte[], Long>)obj;

        if (result == null) {
            returnValue(ChitterOperations.FollowerChangeResult.FAILURE);
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
                returnValue(ChitterOperations.FollowerChangeResult.ALREADY_EXISTS);
                return;
            }
        }

        String line = String.format(
            "%s\t%d\n", username, System.currentTimeMillis()
        );
        
        doThen(
            Invocation.of(
                FSOperations.class, "appendIfNotChanged",
                followingFn, line.getBytes(), version
            ),
            Invocation.on(this, "setFollowingV")
        );
    }

    private long followingV;
    public void setFollowingV(Object obj) throws Exception {
        followingV = (Long)obj;
        if (followingV == FAILURE) {
            returnValue(ChitterOperations.FollowerChangeResult.FAILURE);
            return;
        }

        returnValue(ChitterOperations.FollowerChangeResult.SUCCESS);
        return;
    }
}
