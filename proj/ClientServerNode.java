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
    @Client protected FSCache fsCache;

    public ClientServerNode() {
        fs = new LocalFS(this);
        fsCache = new FSCache();

        sendQueue = new LinkedList<Request>();
        recvQueue = new LinkedList<Request>();
        pendingRequests = new HashMap<Request, Request>();
    }

    @Server protected void handleRequest(int from, byte[] msg) {
        Request req = null;
        byte[] out = {};

        try {
            req = (Request)Serialization.decode(msg);
            if (req.getInvokable() instanceof Transaction) {
                Transaction t = (Transaction)req.getInvokable();
                FSTransaction fst = new FSTransaction(t, (LocalFS)fs);
                fst.invokeOn(fs);
                // sorta ugly: reach in and deposit return value in the transaction
                // we'll send back, altenatively we could send back the FSTransaction
                // itself, but that also seems ugly... meh.
                t.setReturnValue(fst.getReturnValue());
            } else {
                req.getInvokable().invokeOn(fs);
            }
            out = Serialization.encode(req);
            logOutput(req.toString());
        } catch (Serialization.DecodingException e) {
            logOutput("Failed to decode RPC request.");
            return;
        } catch (InvocationException e) {
            try {
                //req.getInvokable().setReturnValue(null);
                out = Serialization.encode(req);
                RIOSend(from, Protocol.CHITTER_RPC_REPLY, out);
                return;
            } catch(Exception ee) {
                logOutput(e.toString() + " " + req);
                return;
            }
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
        if (this.sendQueue.peek() != null) {
            logOutput("sendQueue = " + this.sendQueue.toString());
        }

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
        }
    }

    @Client protected void pumpRecvQueue() {
        if (this.recvQueue.peek() != null) {
            logOutput("recvQueue = " + this.recvQueue.toString());
        }

        Request req = this.recvQueue.poll();
        if (req == null) {
            return;
        }

        Request request = this.pendingRequests.get(req);
        this.pendingRequests.remove(req);
        if (request != null) {
            try {
                if (req.getInvokable().getReturnValue() == null) {
                    onCommandCompletion(request);
                } else {
                    request.getInvokable().setReturnValue(
                        req.getInvokable().getReturnValue()
                    );
                    request.complete();
                    if (!hasOutstandingRequests()) {
                        onCommandCompletion(request);
                    }
                }
            } catch (InvocationException e) {
                logError(e.toString() + " " + request);
                e.printStackTrace();
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
                method = /*frame.getClassName() + "::" + */frame.getMethodName();
            }
        }

        stream.format("Node %d %s: %s\n", addr, method, output);
    }

    @Client public boolean hasOutstandingRequests() {
        return !this.pendingRequests.isEmpty();
    }

    @Client public void onRIODrop(byte[] payload) {
        try {
            Request req = (Request)Serialization.decode(payload);
            // simply re-send it for now
            this.sendQueue.offer(req);
            pumpSendQueue();
        } catch(Serialization.DecodingException e) {
            logOutput("Failed to decode dropped RPC request.");
        }
    }

    @Client public abstract void onCommandCompletion(Request r);

    @Client public FSCache getCache() {
        return this.fsCache;
    }
}
