import org.python.core.*;
import org.python.util.*;

/**
 * Works with console.pl to provide a Python REPL with the right import path.
 */
public class Repl {
    private static final String PYTHON_HOME = "python.home";

    public static void main(String[] args) throws Exception {
        if (System.getProperty(PYTHON_HOME) == null) {
            System.setProperty(PYTHON_HOME, "");
        }

        PySystemState sys = Py.getSystemState();
        sys.path.append(new PyString("proj/"));
        sys.path.append(new PyString("lib/edu/washington/cs/cse490h/lib/"));

        InteractiveConsole.initialize(
            System.getProperties(), null, new String[0]
        );
        InteractiveConsole c = new InteractiveConsole();
        c.runsource("from edu.washington.cs.cse490h.lib import Utility");
        c.runsource("from java.lang import *");
        c.runsource("from java.util import *");
        c.runsource("Utility.setRNG(Random())");
        c.interact();
    }
}
