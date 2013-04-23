import java.lang.reflect.Method;
import java.util.*;
import java.io.*;

import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Node;
import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;


/** A simple filesystem command, this will be a single RPC, and we
 *  won't even do anything with the results */
public class FSCommand extends Command {

    private Invocation operation;
    private boolean executed;
    private int destination;
    private Pair<Invocation, Integer> result;
    private boolean failed;

    public FSCommand(ChitterNode n, Invocation inv, int dest) {
        super(n);
        this.operation = inv;
        this.destination = dest;
        this.executed = false;
        this.failed = true;
    }

    public void execute(Queue<Pair<Invocation, Integer>> results) {
        if (!executed) {
            node.sendRPC(operation, destination);
            executed = true;
        }

        if (!results.isEmpty()) {
            result = results.poll();
            failed = result == null;
        }
    }

    public boolean hasCompleted() {
        return result != null;
    }

    public boolean hasFailed() {
        return failed;
    }
}
