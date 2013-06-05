import java.util.*;

/** Some helpers for dealing with Transactions in Invocations */
public class InvokableUtils {

    private static Set<String> readOps = new HashSet<String>() {{
        add("read");
        add("exists");
        add("currentVersion");
        add("hasChanged");
        add("isSameVersion");
    }};

    /** Is a given FS operation a mutation? */
    public static boolean mutates(String name) {
        return !readOps.contains(name);
    }
    
    /** Is an Invokable read-only? */
    public static boolean isReadOnly(Invokable iv) {
        Invocation i = (Invocation)iv;
        return !mutates(i.getMethodName());
    }
}
