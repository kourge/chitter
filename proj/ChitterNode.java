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
    public static int RPC_REPLY_TIMEOUT = 15;

    PersistentStorageWriter log;

    // Server
    ChitterFSOperations fsOps;

    // Client
    Invocation pendingRPC; // A pending RPC call
    int pendingRPCSeq;     // seq number, so we know when it fails :(
    Queue<Pair<Invocation, Integer> > pendingRPCs; // rpcs that we
                                                   // should send next
    Queue<Pair<Invocation, Integer> > rpcReplies; // replies to commands in the
                                                  // order they were invoked

    /**
     * Create a new node and initialize everything
     */
    public ChitterNode() {
        fsOps = new ChitterFSOperations(this);
        pendingRPC = null;
        pendingRPCs = new LinkedList<Pair<Invocation, Integer> >();
        rpcReplies = new LinkedList<Pair<Invocation, Integer> >();
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

        Invocation iv;
        if (cmd.equals("create")
            || cmd.equals("exists")
            || cmd.equals("delete")
            || cmd.equals("read")) {
            String filename = s.next();
            iv = Invocation.of(ChitterFSOperations.class, cmd, filename);
        } else if (cmd.equals("hasChanged")) {
            String filename = s.next();
            long v = s.nextLong();
            iv = Invocation.of(ChitterFSOperations.class, cmd, filename, v);
        } else if (cmd.equals("appendIfNotChanged")
                   || cmd.equals("overwriteIfNotChanged")) {
            String filename = s.next();
            long v = s.nextLong();
            String payload;
            int startIdx = command.indexOf('\"');
            int endIdx = command.lastIndexOf('\"');
            if (startIdx == endIdx || startIdx < 0 || endIdx < 0) {
                payload = "";
            } else {
                payload = command.substring(startIdx + 1, endIdx);
            }
            iv = Invocation.of(ChitterFSOperations.class, cmd, filename,
                payload.getBytes(), v);
        } else {
            return false;
        }

        return sendRPC(iv, destination);
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
                byte[] out;

                try {
                    iv = (Invocation)Serialization.decode(msg);
                    iv.invokeOn(fsOps);
                    out = Serialization.encode(iv);
                } catch (Serialization.DecodingException e) {
                    logOutput("Failed to decode RPC request.");
                    return;
                } catch (InvocationException e) {
                    logOutput("RPC invocation failed. " + e);
                    return;
                } catch (Serialization.EncodingException e) {
                    logOutput("Failed to encode RPC response.");
                    return;
                }

                // send it back (including return value)
                RIOSend(from, Protocol.CHITTER_RPC_REPLY, out);
                break;
            case Protocol.CHITTER_RPC_REPLY:
                Invocation rpcResult;
                try {
                    rpcResult = (Invocation)Serialization.decode(msg);
                } catch (Serialization.DecodingException e) {
                    logOutput("Failed to decode RPC reply.");
                    return;
                }

                if (pendingRPC == null || !pendingRPC.getMethodName().equals(
                    rpcResult.getMethodName())) {
                    logOutput("Unexpected RPC reply: "
                        + rpcResult.getMethodName()
                        + " expected: " + pendingRPC.getMethodName());
                    return;
                }

                logOutput("RPC reply received: " + rpcResult.getMethodName());
                // add to the reply queue so the caller can consume it
                rpcReplies.add(Pair.of(rpcResult, from));
                pendingRPC = null;

                // if there were rpcs queued up, then run the next one now
                if (!pendingRPCs.isEmpty()) {
                    Pair<Invocation, Integer> cmd = pendingRPCs.poll();
                    Invocation inv = cmd.first();
                    int destination = cmd.second();
                    byte[] payload;
                    try {
                        payload = Serialization.encode(inv);
                    } catch (Serialization.EncodingException e) {
                        logOutput("Failed to encode RPC request.");
                        return;
                    }
                    pendingRPC = inv;
                    RIOSend(destination, Protocol.CHITTER_RPC_REQUEST, payload);
                }

                break;
            default:
                logOutput("Unknown protocol packet: " + protocol);
        }
    }

    @Override
	public void onRIODrop(int seqNum) {
        if (seqNum == pendingRPCSeq) {
            rpcReplies.add(null);
            pendingRPC = null;
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

    private boolean sendRPC(Invocation iv, int destination) {
        if (pendingRPC != null) {
            pendingRPCs.add(Pair.of(iv, destination));
        } else {
            byte[] payload;
            try {
                payload = Serialization.encode(iv);
            } catch (Serialization.EncodingException e) {
                logOutput("Failed to encode RPC request.");
                return false;
            }
            pendingRPC = iv;

            pendingRPCSeq = RIOSend(destination, Protocol.CHITTER_RPC_REQUEST,
                                    payload);
            try {
                Method onTimeoutMethod = Callback.getMethod("onTimeoutRPC", this,
                                                            new String[] {});
                addTimeout(new Callback(onTimeoutMethod, this, new Object[] {}),
                           RPC_REPLY_TIMEOUT);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
        return true;
    }

    public void onTimeoutRPC() {
        rpcReplies.add(null);
        pendingRPC = null;
    }
}
