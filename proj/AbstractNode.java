import java.util.Map;
import java.util.HashMap;
import org.python.core.*;
import org.python.util.PythonInterpreter;

/**
 * A superclass that PaxosNode subclasses. This is needed because Jython
 * classes cannnot override static methods, among other things.
 */
public abstract class AbstractNode extends RIONode {
    public FS fs;

    public AbstractNode() {
        this.fs = new LocalFS(this);
    }

    public static double getFailureRate() { return 0.0; }
    public static double getRecoveryRate() { return 1.0; }
    public static double getDropRate() { return 0.0; }
    public static double getDelayRate() { return 0.0; }

    public static Map<String, String> consoleOperationsDescription;
    static {
        PythonInterpreter python = new PythonInterpreter();
        python.exec("from pyoperation import RemoteOp as Op");
        PyType remoteOp = (PyType)python.get("Op");

        consoleOperationsDescription = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        Map docs = (Map)remoteOp.invoke("__docs__".intern());
        for (Object key : docs.keySet()) {
            consoleOperationsDescription.put((String)key, (String)docs.get(key));
        }
    }
}
