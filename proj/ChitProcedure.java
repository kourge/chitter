import edu.washington.cs.cse490h.lib.Utility;
import java.util.Scanner;
import java.util.Arrays;

public class ChitProcedure extends ChitterProcedure {
    private ChitProcedure() {}

    @Proc(name="chit", desc="chit username text")
    public static ChitProcedure make(
        ClientServerNode node, Invocation onComplete, int destination,
        String commandString
    ) {
        Scanner scanner = new Scanner(commandString);
        String username = scanner.next();
        String text = scanner.nextLine();

        return new ChitProcedure(
            node, onComplete, destination, username, text
        );
    }

    private Chit chit;
    private byte[] stream;
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

        stream = Arrays.copyOf(stream, stream.length + 1);
        stream[stream.length - 1] = (byte)'\n';

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
                tweetsFn, stream, version
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
