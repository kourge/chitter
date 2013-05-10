import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Scanner;

import org.python.core.*;
import org.python.util.PythonInterpreter;

public class Operation {
    private static PyType remoteOp;
    private static PyType mockFS;
    static {
        PythonInterpreter python = new PythonInterpreter();
        python.exec("from operation import RemoteOp");
        python.exec("from fs import MockFS");
        remoteOp = (PyType)python.get("RemoteOp");
        mockFS = (PyType)python.get("MockFS");
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

        PyObject fs = mockFS.__call__();
        PyObject instance = remoteOp.__call__(fs);
        PyGenerator proc = (PyGenerator)instance.__call__(
            new PyString(commandName), new PyString(commandString)
        );

        PyObject value = proc.next();
        Object javaVal = value.__tojava__(Invocation.class);

        if (javaVal instanceof Invocation) {
            Request req = Request.to(
                this.destination, (Invocation)javaVal,
                Invocation.on(this, "onRequestComplete")
            );

            pendingOps.put(req, proc);
            this.node.sendRPC(req);
        } else {
            // We have arrived at our final value
            System.out.println("Got PyObject as result: " + value);
        }
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
        Object javaVal = value.__tojava__(Invocation.class);

        if (javaVal instanceof Invocation) {
            req = Request.to(
                this.destination, (Invocation)javaVal,
                Invocation.on(this, "onRequestComplete")
            );

            pendingOps.put(req, proc);
            this.node.sendRPC(req);
        } else {
            // We have arrived at our final value
            System.out.println("Got PyObject as result: " + value);
        }
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
