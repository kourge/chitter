import edu.washington.cs.cse490h.lib.Node;

import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;

public class GetChitsProcedure extends ChitterProcedure {
    private GetChitsProcedure() {}

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
            Invocation.of(FSOperations.class, "read", tweetsFn),
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
        String lines = new String(result.first());

        Scanner scanner = new Scanner(lines);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Chit chit;
            try {
                chit = (Chit)Serialization.decode(line.getBytes());
            } catch (Serialization.DecodingException e) {
                returnValue(null);
                return;
            }
            list.add(chit);
        }

        returnValue(list);
        return;
    }
}
