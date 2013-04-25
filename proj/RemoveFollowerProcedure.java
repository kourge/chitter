import edu.washington.cs.cse490h.lib.Node;

import java.util.Scanner;

public class RemoveFollowerProcedure extends ChitterProcedure {
    private RemoveFollowerProcedure() {}

    private String username;
    private String followingFn;

    public RemoveFollowerProcedure(
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
            returnValue(ChitterOperations.FollowerChangeResult.FAILURE);
            return;
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
            returnValue(ChitterOperations.FollowerChangeResult.DOES_NOT_EXIST);
            return;
        }
        
        doThen(
            Invocation.of(
                fs, "overwriteIfNotChanged",
                followingFn, out.toString().getBytes(), version
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
