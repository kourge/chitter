import java.lang.reflect.Method;
import java.util.*;
import java.io.*;

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
    public static int TIMEOUT = 5;

    PersistentStorageWriter log;
    ChitterFSOperations fsOps;

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
        fsOps = new ChitterFSOperations(this);
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
        if (matchFSOperation(command)) {
            return;
        }
        logError("Unrecognized command: " + command);
    }

    private boolean matchFSOperation(String command) {
        Scanner s = new Scanner(command);
        String cmd = s.next();
        int destination = s.nextInt();
        byte[] payload;

        Invocation iv;
        if (cmd.equals("create")) {
            String filename = s.next();
            iv = Invocation.of(ChitterFSOperations.class, "create", filename);
        } else {
            // TODO the rest of the operations
            return false;
        }
        try {
            payload = Serialization.encode(iv);
        } catch(Exception e) {
            logOutput("Failed to encode RPC request.");
            return false;
        }
        RIOSend(destination, Protocol.CHITTER_RPC_REQUEST, payload);
        return true;
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
        switch(protocol) {
            case Protocol.CHITTER_RPC_REQUEST:
                // we've been sent an RPC, we should invoke it..
                logOutput("RPC request received.");
                Invocation iv;

                try {
                    iv = (Invocation)Serialization.decode(msg);
                } catch(Exception e) {
                    logOutput("Failed to decode RPC request.");
                    return;
                }

                try {
                    iv.invokeOn(fsOps);
                } catch (Exception e) {
                    logOutput("RPC invokation failed.");
                    return;
                }
                
                byte[] out;
                try {
                    out = Serialization.encode(iv);
                } catch(Exception e) {
                    logOutput("Failed to encode RPC response.");
                    return;
                }

                // send it back (including return value)
                RIOSend(from, Protocol.CHITTER_RPC_REPLY, out);
                break;
            case Protocol.CHITTER_RPC_REPLY:
                // this is the reply to an RPC that we invoked, we should
                // handle it
                logOutput("RPC reply received.");
                break;
            default:
                logOutput("Unknown protocol packet: " + protocol);
        }
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
