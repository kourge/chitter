import edu.washington.cs.cse490h.lib.Node;

public class CreateUserProcedure extends ChitterProcedure {
    private CreateUserProcedure() {}

    private String tweetsFn;
    private String followingFn;
    private String userFn;

    public CreateUserProcedure(
        ClientServerNode node, Invocation onComplete, int destination,
        String username
    ) {
        populate(node, onComplete, destination);

        checkUsername(username);

        tweetsFn = "tweets:" + username;
        followingFn = "following:" + username;
        userFn = "users:" + username;
    }

    public void call() throws Exception {
        doThen(
            Invocation.of(fs, "create", tweetsFn),
            Invocation.on(this, "setTweetsFn")
        );
    }
    
    private long tweetsV;
    public void setTweetsFn(Object obj) throws Exception {
        tweetsV = (Long)obj;
        if (tweetsV == FAILURE) {
            returnValue(false);
            return;
        }

        doThen(
            Invocation.of(fs, "create", followingFn),
            Invocation.on(this, "setFollowingV")
        );
    }

    private long followingV;
    public void setFollowingV(Object obj) throws Exception {
        followingV = (Long)obj;
        if (followingV == FAILURE) {
            returnValue(false);
            return;
        }

        doThen(
            Invocation.of(fs, "create", userFn),
            Invocation.on(this, "setUserV")
        );
    }

    private long userV;
    public void setUserV(Object obj) throws Exception {
        userV = (Long)obj;
        if (userV == FAILURE) {
            returnValue(false);
            return;
        }

        doThen(
            Invocation.of(fs, "currentVersion", userFn),
            Invocation.on(this, "setVersion")
        );
    }

    private long version;
    public void setVersion(Object obj) throws Exception {
        version = (Long)obj;
        String content = String.format("%d", 0);

        doThen(
            Invocation.of(
                fs, "overwriteIfNotChanged",
                userFn, content.getBytes(), version
            ),
            Invocation.on(this, "setUserV2")
        );
    }

    public void setUserV2(Object obj) throws Exception {
        userV = (Long)obj;
        if (userV == FAILURE) {
            returnValue(false);
            return;
        }

        returnValue(true);
        return;
    }
}
