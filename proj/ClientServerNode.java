import java.util.*;
import java.io.*;
import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Utility;
import java.lang.reflect.Method;

public abstract class ClientServerNode extends RIONode {
    protected @interface Client {}
    protected @interface Server {}

    @Server protected FS fs;

    @Client protected Queue<Request> sendQueue;
    @Client protected Queue<Request> recvQueue;
    @Client protected Map<Request, Request> pendingRequests;
    @Client protected byte[] rpcPayload;

    public ClientServerNode() {
        fs = new LocalFS(this);

        sendQueue = new LinkedList<Request>();
        recvQueue = new LinkedList<Request>();
        pendingRequests = new HashMap<Request, Request>();
    }

    @Server protected void handleRequest(int from, byte[] msg) {
        Request req = null;
        byte[] out = {};

        try {
            req = (Request)Serialization.decode(msg);
            req.getInvocation().invokeOn(fs);
            out = Serialization.encode(req);
        } catch (Serialization.DecodingException e) {
            logOutput("Failed to decode RPC request.");
            return;
        } catch (InvocationException e) {
            logOutput("Invocation failed. " + e);
            return;
        } catch (Serialization.EncodingException e) {
            logOutput("Failed to encode RPC response.");
            return;
        } catch (Exception e) {
            return;
        }

        if (out.length != 0) {
            RIOSend(from, Protocol.CHITTER_RPC_REPLY, out);
        }
    }

    @Client protected void handleReply(int from, byte[] msg) {
        Request req = null;
        try {
            req = (Request)Serialization.decode(msg);
        } catch (Serialization.DecodingException e) {
            logOutput("Failed to decode RPC reply.");
            return;
        }

        this.recvQueue.offer(req);
        pumpRecvQueue();
    }

    @Client protected void pumpSendQueue() {
        logOutput("sendQueue = " + this.sendQueue.toString());

        Request req = this.sendQueue.poll();
        if (req == null) {
            return;
        }

        //byte[] out = {};
        try {
            rpcPayload = Serialization.encode(req);
        } catch (Serialization.EncodingException e) {
            logOutput("Failed to encode request.");
        }

        if (rpcPayload.length != 0) {
            RIOSend(req.getDestination(), Protocol.CHITTER_RPC_REQUEST, rpcPayload);
            this.pendingRequests.put(req, req);
            try {
                Method onTimeoutMethod = Callback.getMethod("onRPCTimeout", this,
                    new String[]{ "Request" });
                addTimeout(new Callback(onTimeoutMethod, this, new Object[]
                    { req }), 5);
            } catch (Exception e) {}
        }
    }

    @Client protected void pumpRecvQueue() {
        Request req = this.recvQueue.poll();
        if (req == null) {
            return;
        }

        Request request = this.pendingRequests.get(req);
        this.pendingRequests.remove(req);
        if (request != null) {
            try {
                request.getInvocation().setReturnValue(req.getInvocation().getReturnValue());
                request.complete();
                if (!hasOustandingRequests()) {
                    onCommandCompletion();
                }
            } catch (InvocationException e) {
                logError("Failed to invoke onComplete callback " + request.getOnComplete().getMethodName());
            }
        } else {
            logError("Received unknown response: " + req);
        }
    }

    @Client public boolean sendRPC(Request req) {
        if (req == null) {
            return false;
        }
        boolean result = this.sendQueue.offer(req);
        pumpSendQueue();
        return result;
    }

    public void logError(String output) {
        log(output, System.err);
    }

    public void logOutput(String output) {
        // log(output, suppressOutput ? System.err : System.out);
        log(output, System.out);
    }

    private static final int LOG_CALL_DEPTH = 2;
    public void log(String output, PrintStream stream) {
        String method = "";
        try {
            throw new Exception();
        } catch (Exception e) {
            StackTraceElement[] trace = e.getStackTrace();
            if (trace.length > LOG_CALL_DEPTH) {
                StackTraceElement frame = trace[LOG_CALL_DEPTH];
                method = frame.getClassName() + "::" + frame.getMethodName();
            }
        }

        stream.format("Node %d %s: %s\n", addr, method, output);
    }

    @Client public boolean hasOustandingRequests() {
        return !this.pendingRequests.isEmpty();
    }

    /**
     * Resend an RPC that we didn't get a reply from in time
     *  */
    @Client public void onRPCTimeout(Request req) {
        if (pendingRequests.containsKey(req)) {
            int addr = req.getDestination();
            RIOSend(addr, Protocol.CHITTER_RPC_REQUEST, rpcPayload);
            try {
                Method onTimeoutMethod = Callback.getMethod("onRPCTimeout", this,
                    new String[]{ "Request" });
                addTimeout(new Callback(onTimeoutMethod, this, new Object[]
                    { req }), 5);
            } catch (Exception e) {}
        }
    }

    @Client public abstract void onCommandCompletion();
}
