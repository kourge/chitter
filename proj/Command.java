import java.lang.reflect.Method;
import java.util.*;
import java.io.*;

import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Node;
import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;


/** A command object, composed of a series of RPCs and application
 *  logic. */
public abstract class Command {
    
    protected ChitterNode node;
    
    public Command(ChitterNode n) {
        this.node = n;
    }

    /** update our state, given most recent RPC results (nulls indicate
     *  failed RPCs) */
    public abstract void execute(Queue<Pair<Invocation, Integer>> results);

    /** Should return true iff it 100% sucessfully completed */
    public abstract boolean hasCompleted();

    /** Should return true if an RPC failed or some other error condition */
    public abstract boolean hasFailed();
}
