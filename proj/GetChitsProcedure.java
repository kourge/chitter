import edu.washington.cs.cse490h.lib.Utility;

import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Arrays;

public class GetChitsProcedure extends ChitterProcedure {
    private GetChitsProcedure() {}

    @Proc(name="getChits", desc="getChits username")
    public static GetChitsProcedure make(
        ClientServerNode node, Invocation onComplete, int destination,
        String commandString
    ) {
        Scanner scanner = new Scanner(commandString);
        String username = scanner.next();

        return new GetChitsProcedure(
            node, onComplete, destination, username
        );
    }

    private List<Chit> list;
    private String tweetsFn;

    public GetChitsProcedure(
        ClientServerNode node, Invocation onComplete, int destination,
        String username
    ) {
        populate(node, onComplete, destination);

        checkUsername(username);

        list = new LinkedList<Chit>();
        tweetsFn = "tweets:" + username;
    }

    public void call() throws Exception {
        doThen(
            Invocation.of(fs, "read", tweetsFn),
            Invocation.on(this, "setResult")
        );
    }
    
    private Pair<byte[], Long> result;
    @SuppressWarnings("unchecked")
    public void setResult(Object obj) throws Exception {
        result = (Pair<byte[], Long>)obj;

        if (result == null) {
            returnValue(null);
            return;
        }
        byte[] stream = result.first();

        int last = 0;
        for (int i = 0; i < stream.length; i++) {
            if (stream[i] != (byte)'\n') {
                continue;
            }
            byte[] line = Arrays.copyOfRange(stream, last, i);

            Chit chit;
            try {
                chit = (Chit)Serialization.decode(line);
            } catch (Serialization.DecodingException e) {
                returnValue(null);
                return;
            }
            list.add(chit);

            last = i + 1;
        }

        returnValue(list);
        return;
    }
}
