import java.lang.reflect.Method;
import java.util.*;
import java.io.*;

import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Node;
import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;

public class ChitterNode extends ClientServerNode {
    // override the default failure rates (never fail for now)
    public static double getFailureRate() { return 0.0; }
    public static double getRecoveryRate() { return 1.0; }
    public static double getDropRate() { return 0.0; }
    public static double getDelayRate() { return 0.0; }

    public static int NUM_NODES = 2;// client and server only for now
    public static int TIMEOUT = 5;
    public static int RPC_REPLY_TIMEOUT = 50;

    protected PersistentStorageWriter log;

    public static Map<String, String> consoleOperationsDescription = Operation.getOperations();
    private static Queue<String> pendingCommands;

    /**
     * Create a new node and initialize everything
     */
    public ChitterNode() {
        super();
        pendingCommands = new LinkedList<String>();
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
                logOutput("Recovering. Checking logs...");
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
        try {
            PersistentStorageReader logReader = getReader("log");
            Queue<String> cmds = new LinkedList<String>();

            String tmp;
            while ((tmp = logReader.readLine()) != null) {
                if (tmp.equals("COMPLETE")) {
                    cmds.poll();
                } else {
                    cmds.add(tmp);
                }
            }

            // Restart all unfinished commands
            while (!cmds.isEmpty()) {
                String command = cmds.poll();
                queueDirective(command);
            }

            log = getWriter("log", true);
        } catch (Exception e) {
            logOutput("Failed during recovery");
            fail();
        }
    }


    @Override
    public void fail() {
        logOutput("failed");
        super.fail();
    }

    @Override
    public void onCommand(String command) {
        try {
            log.append(command + "\n");
        } catch (IOException e) {
            // well, shit, if we failed here, we drop the command completely
            // but this is okay with at most once semantics, so whatevs...
            logOutput("Failed to write command to logfile: " + command);
            fail();
        }

        queueDirective(command);
    }

    public void queueDirective(String directive) {
        if (hasOustandingRequests()) {
            pendingCommands.offer(directive);
        } else {
            Scanner scanner = new Scanner(directive);
            int destination = scanner.nextInt();
            String directiveName = scanner.next();

            if (Command.supports(directiveName)) {
                Request req = Command.asRequest(directive);
                logOutput("request = " + req);
                sendRPC(req);
            } else if (Operation.supports(directiveName)) {
                try {
                    Operation.performOn(this, directive);
                } catch (Exception e) {
                }
            }
            pumpSendQueue();
        }
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
        switch (protocol) {
        case Protocol.CHITTER_RPC_REQUEST:
            logOutput("RPC request received.");
            this.handleRequest(from, msg);
            break;

        case Protocol.CHITTER_RPC_REPLY:
            this.handleReply(from, msg);
            break;

        default:
            logOutput("Unknown protocol packet: " + protocol);
        }

        pumpRecvQueue();
        pumpSendQueue();
    }

    @Override
    public String toString() {
        String s = "node";
        return s;
    }

    @Override
    @Client public void onCommandCompletion() {
        try {
            log.append("COMPLETE\n");
        } catch (IOException e) {}
        if (!pendingCommands.isEmpty()) {
            queueDirective(pendingCommands.poll());
        }
    }
}

