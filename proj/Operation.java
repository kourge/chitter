import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.python.core.*;
import org.python.util.PythonInterpreter;

public class Operation {
    private static PyType remoteOp;
    private static PyType rpc;
    static {
        PythonInterpreter python = new PythonInterpreter();
        python.exec("from pyoperation import TransactionalRemoteOp");
        python.exec("from pyfs import RPC");
        remoteOp = (PyType)python.get("TransactionalRemoteOp");
        rpc = (PyType)python.get("RPC");
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
        if (Py.isInstance(value, rpc)) {
            Invokable iv = convertToInvokable(value.__call__());
            if (iv == null) {
                System.out.println("Could not convert PyObject to Invokable");
            }

            Request req = Request.to(
                this.destination, iv, Invocation.on(this, "onRequestComplete")
            );
            pendingOps.put(req, proc);
            this.node.sendRPC(req);
        } else {
            // We have arrived at our final value
            System.out.println("Got PyObject as result: " + value);
        }
    }

    public Invokable convertToInvokable(PyObject value) {
        if (false) {
        } else if (value instanceof PyTuple) {
            PyTuple t = (PyTuple)value;
            String name = t.pyget(0).asString();
            PyTuple args = (PyTuple)t.pyget(1);
            return this.argsToInvocation(name, args);
        } else if (value instanceof PyList) {
            // value is a list for a transaction
            PyList operations = (PyList)value;
            Invocation[] ivs = new Invocation[operations.size()];
            for (int i = 0; i < operations.size(); i++) {
                PyTuple t = (PyTuple)operations.pyget(i);
                String name = t.pyget(0).asString();
                PyTuple args = (PyTuple)t.pyget(1);
                ivs[i] = this.argsToInvocation(name, args);
            }
            return new Transaction(ivs);
        }

        return null;
    }

    public Invocation argsToInvocation(String name, PyTuple args) {
        Invocation iv = Invocation.of(FS.class, name);
        Class<?>[] types = iv.getParameterTypes();
        Object[] params = new Object[args.size()];

        for (int i = 0; i < args.size(); i++) {
            params[i] = args.pyget(i).__tojava__(types[i]);
        }
        iv.setParameterValues(params);

        return iv;
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
