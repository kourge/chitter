import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Scanner;

import org.python.core.*;
import org.python.util.PythonInterpreter;

/**
 * Takes a Chitter operation String command and performs it by passing it to an
 * instantiated Python class where all high-level logic takes place.
 *
 * All the Chitter operations are implemented in Python as generators, which are
 * functions that can be paused from within and resumed from the outside.
 * Generators are usually used to dynamically generate values, but for our
 * purposes we use them to represent continuations.
 *
 * In Python, a generator `p` must first be called like a function to initialize
 * it.  To start it, call `p.next()`, which will run the generator until the it
 * hits a `yield x` expression. `x` becomes the return value of the expression
 * `p.next()`. To resume it, call `p.send(y)`, which will resume the generator
 * while setting the value of the `yield x` expression to `y`.
 *
 * This effectively lets a function cooperatively pause itself and wait for
 * a network call to complete without the use of threads.
 */
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
    private PyObject prevValue;

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

        if (InvokableUtils.isReadOnly(req.getInvokable()) && prevValue != null) {
            // assume it was a single invocation for now, and not a whole transaction
            // TODO: batch reads into transactions below and account for that here?
            Invocation i = (Invocation)req.getInvokable();

            @SuppressWarnings("unchecked")
            Pair<byte[], Long> out = (Pair<byte[], Long>)i.getReturnValue();
            this.node.getCache().put((String)i.getParameterValues()[0], out);
            this.handleValue(this.prevValue, proc);
        } else {
            // we weren't a cache read, pass along to the generator
            PyObject value;

            Invokable iv = (Invokable)req.getInvokable();
            
            if (iv instanceof Batch) {
                Batch t = (Batch)iv;
                value = proc.send(Py.java2py(t));
            } else {
                value = proc.send(Py.java2py(result));
            }

            this.handleValue(value, proc);
        }
    }

    public void handleValue(PyObject value, PyGenerator proc) {
        this.prevValue = value;
        if (Py.isInstance(value, rpc)) {
            Invokable iv = convertToInvokable(value.__call__());
            if (iv == null) {
                System.out.println("Could not convert PyObject to Invokable");
            }

            if (InvokableUtils.isReadOnly(iv)) {
                if (iv instanceof Invocation) {
                    Invocation i = (Invocation)iv;
                    Object result = this.readCached(i, proc);
                    if (result == null) {
                        System.out.println("Cache miss: " + (String)i.getParameterValues()[0]);
                        return;
                    } else {
                        this.prevValue = null;
                        PyObject v = proc.send(Py.java2py(result));
                        this.handleValue(v, proc);
                    }
                } else if (iv instanceof Batch) {
                    Batch t = (Batch)iv;
                    for (Invocation i : t.getInvocations()) {
                        Object result = this.readCached(i, proc);
                        if (result == null) {
                            System.out.println("Cache miss: " + (String)i.getParameterValues()[0]);
                            return;
                        } else {
                            i.setReturnValue(result);
                        }
                    }
                    this.prevValue = null;
                    // if we made it this far we had everything in cache, and can continue
                    PyObject v = proc.send(Py.java2py(t));
                    this.handleValue(v, proc);
                } else { /* ??? */}
            } else {
                // if it has writes, just always send to the server
                Request req = Request.to(
                    this.destination, iv, Invocation.on(this, "onRequestComplete")
                );
                pendingOps.put(req, proc);
                this.node.sendRPC(req);
            }
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
            return new Batch(ivs);
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

    private Object readCached(Invocation i, PyGenerator proc) {
        Pair<byte[], Long> result;
        String name = (String)i.getParameterValues()[0];
        if ((result = this.node.getCache().get(name)) != null) {
            String op = i.getMethodName();
            if (op.equals("read")) {
                return result;
            } else if (op.equals("exists")) {
                return result != null;
            } else if (op.equals("currentVersion")) {
                return result.second();
            } else if (op.equals("hasChanged")) {
                return result.second().equals(i.getParameterValues()[1]);
            }
            return null;
        } else {
            // fire off a read and we'll have it next time
            Invocation inv = Invocation.of(FS.class, "read", name);
            Request req = Request.to(
                this.destination, inv, Invocation.on(this, "onRequestComplete")
            );
            pendingOps.put(req, proc);
            this.node.sendRPC(req);
            return null;
        }
    }
}
