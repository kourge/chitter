import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;

import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Node;
import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;

public class ChitterNode extends RIONode {
    // override the default failure rates (never fail for now)
    public static double getFailureRate() { return 0.0; }
    public static double getRecoveryRate() { return 0.0; }
    public static double getDropRate() { return 0.0; }
    public static double getDelayRate() { return 0.0; }

    public static int NUM_NODES = 2;// client and server only for now
    public static int TIMEOUT = 3;

    PersistentStorageWriter log;

    enum State {
        IDLE,
        CREATE,
        EXISTS,
        READ,
        APPEND,
        APPEND_IF_CHANGED,
        OVERWRITE_IF_CHANGED,
        HAS_CHANGED,
        DELETE
    };

    private State currentState;

    /**
     * Create a new node and initialize everything
     */
    public ChitterNode() {
        currentState = State.IDLE;
    }

    /**
     * Called by the manager to start this node up.
     */
    @Override
    public void start() {
        try {
            if (!Utility.fileExists(this, "log")) {
                // First start of node
                log = getWriter("log", false);
                logOutput("Started fresh.");
            } else {
                // We are a recovering node
                logOutput("Recovered. Checking logs...");
                recoverWithLog();
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Recover using a log saved in persistent storage. We will attempt to use
     * the log to decide.
     * 
     * @throws IOException
     */
    private void recoverWithLog() throws IOException {
        PersistentStorageReader logReader = getReader("log");
        // ...
    }

    @Override
    public void fail() {
        logOutput("failed");

        // Please call this at the end of stop
        super.fail();
    }

    @Override
    public void onCommand(String command) {
        logError("Unrecognized command: " + command);
    }

    /**
     * Actually process a packet.
     * 
     * @param from
     *            The address of the sender
     * @param protocol
     *            The protocol identifier of the message
     * @param msg
     *            The message object that was sent
     */
    @Override
	public void onRIOReceive(Integer from, int protocol, byte[] msg) {
        // ...
    }

    @Override
    public String toString() {
        String s = "node";
        return s;
    }

    public void logError(String output) {
        log(output, System.err);
    }

    public void logOutput(String output) {
        log(output, System.out);
    }

    public void log(String output, PrintStream stream) {
        stream.println("Node " + addr + ": " + output);
    }
}
