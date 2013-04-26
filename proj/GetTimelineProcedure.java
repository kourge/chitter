import edu.washington.cs.cse490h.lib.Node;
import edu.washington.cs.cse490h.lib.Utility;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Collections;

public class GetTimelineProcedure extends ChitterProcedure {
    private GetTimelineProcedure() {}

    List<Chit> list;
    String userFn;
    String username;

    public GetTimelineProcedure(
        ClientServerNode node, Invocation onComplete, int destination,
        String username
    ) {
        populate(node, onComplete, destination);

        checkUsername(username);

        list = new ArrayList<Chit>();
        String userFn = "users:" + username;
        this.username = username;
    }

    public void call() throws Exception {
        doThen(
            Invocation.of(fs, "read", userFn),
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

        // TODO: make subprocedure calling work
        // Scanner scanner = new Scanner(new String(result.first()));
        // long timestamp = scanner.nextLong();

        // List<Pair<String, Long>> followings = getFollowings(username);
        // for (Pair<String, Long> following : followings) {
        //     List<Chit> chits = getChits(following.first());
        //     if (chits != null) {
        //         long cutoff = Math.max(following.second(), timestamp);
        //         for (Chit chit : chits) {
        //             if (chit.getTimestamp() > cutoff) {
        //                 list.add(chit);
        //             }
        //         }
        //     }
        // }

        Long timestamp = System.currentTimeMillis();
        byte[] payload = Long.toString(timestamp).getBytes();

        doThen(
            Invocation.of(fs, "overwriteIfNotChanged", userFn, payload, -1),
            Invocation.on(this, "setVersion")
        );
    }

    private Long version;
    public void setVersion(Object obj) throws Exception {
        version = (Long)obj;

        if (version == -1) {
            ;
        }

        Collections.sort(list, new Chit.Comparator());

        returnValue(list);
        return;
    }
}
