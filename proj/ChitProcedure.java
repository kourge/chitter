import edu.washington.cs.cse490h.lib.Node;

public class ChitProcedure extends ChitterProcedure {
    private ChitProcedure() {}

    private Chit chit;
    private byte[] stream;
    private String line;
    private String tweetsFn;

    public ChitProcedure(
        ClientServerNode node, Invocation onComplete, int destination,
        String username, String text
    ) {
        populate(node, onComplete, destination);

        checkUsername(username);

        chit = new Chit(text);
        try {
            stream = Serialization.encode(chit);
        } catch (Serialization.EncodingException e) {
            throw new IllegalArgumentException();
        }

        line = new String(stream) + "\n";
        tweetsFn = "tweets:" + username;
    }

    public void call() throws Exception {
        doThen(
            Invocation.of(fs, "currentVersion", tweetsFn),
            Invocation.on(this, "setVersion")
        );
    }
    
    private long version;
    public void setVersion(Object obj) throws Exception {
        version = (Long)obj;

        doThen(
            Invocation.of(
                fs, "appendIfNotChanged",
                tweetsFn, line.getBytes(), version
            ),
            Invocation.on(this, "setTweetsV")
        );
    }

    private long tweetsV;
    public void setTweetsV(Object obj) throws Exception {
        tweetsV = (Long)obj;
        if (tweetsV == FAILURE) {
            returnValue(false);
            return;
        }

        returnValue(true);
        return;
    }
}
