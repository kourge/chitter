import edu.washington.cs.cse490h.lib.Node;
import edu.washington.cs.cse490h.lib.Utility;

import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;

public class GetFollowingsProcedure extends ChitterProcedure {
    private GetFollowingsProcedure() {}

    String followingFn;
    List<Pair<String, Long>> list;

    public GetFollowingsProcedure(
        ClientServerNode node, Invocation onComplete, int destination,
        String username
    ) {
        populate(node, onComplete, destination);

        checkUsername(username);

        followingFn = "following:" + username;
        list = new LinkedList<Pair<String, Long>>();
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
            returnValue(null);
            return;
        }

        Scanner scanner = new Scanner(new String(result.first()));
        while (scanner.hasNextLine()) {
            String followed = scanner.next();
            long timestamp = scanner.nextLong();
            list.add(Pair.of(followed, timestamp));
        }

        returnValue(list);
        return;
    }
}
