import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Scanner;

import org.python.core.*;
import org.python.util.PythonInterpreter;

public class Operation {
    private static PyType remoteOp;
    static {
        PythonInterpreter python = new PythonInterpreter();
        python.exec("from pyoperation import TransactionalRemoteOp");
        remoteOp = (PyType)python.get("TransactionalRemoteOp");
    }

    private static Map<Request, PyGenerator> pendingOps;
    static {
        pendingOps = new HashMap<Request, PyGenerator>();
    }

    private ClientServerNode node;
    private int destination;

    public Operation(ClientServerNode node) {
        this.node = node;
    }

    public void perform(String command)
    throws Exception {
        Scanner scanner = new Scanner(command);

        this.destination = scanner.nextInt();
        String commandName = scanner.next();
        String commandString = scanner.nextLine();

        PyObject instance = remoteOp.__call__();
        PyGenerator proc = (PyGenerator)instance.__call__(
            new PyString(commandName), new PyString(commandString)
        );

        PyObject value = proc.next();
        this.handleValue(value, proc);
    }

    public void onRequestComplete(Object result, Object request) throws Exception {
        Request req = (Request)request;
        PyGenerator proc = pendingOps.get(req);

        if (proc == null) {
            System.out.printf(
                "onRequestComplete: couldn't find continuation for %s\n",
                req, result
            );
            return;
        }

        pendingOps.remove(req);

        PyObject value = proc.send(Py.java2py(result));
        this.handleValue(value, proc);
    }

    public void handleValue(PyObject value, PyGenerator proc) {
        if (value instanceof PyTuple) {
            Request req = convertToRequest((PyTuple)value);

            pendingOps.put(req, proc);
            this.node.sendRPC(req);
        } else {
            // We have arrived at our final value
            System.out.println("Got PyObject as result: " + value);
        }
    }

    public Request convertToRequest(PyTuple t) {
        String name = t.pyget(0).asString();
        PyTuple args = (PyTuple)t.pyget(1);

        Invocation iv = Invocation.of(FS.class, name);
        Class<?>[] types = iv.getParameterTypes();
        Object[] params = new Object[args.size()];

        for (int i = 0; i < args.size(); i++) {
            params[i] = args.pyget(i).__tojava__(types[i]);
        }
        iv.setParameterValues(params);

        return Request.to(
            this.destination, iv, Invocation.on(this, "onRequestComplete")
        );
    }

    private static Map<String, String> operations;
    static {
        operations = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        Map docs = (Map)remoteOp.invoke("__docs__".intern());
        for (Object key : docs.keySet()) {
            operations.put((String)key, (String)docs.get(key));
        }
    }

    public static Map<String, String> getOperations() {
        return operations;
    }

    public static boolean supports(String operationName) {
        return operations.containsKey(operationName);
    }
}
