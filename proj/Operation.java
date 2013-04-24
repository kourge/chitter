import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;
import java.util.NoSuchElementException;

public enum Operation {
    CREATE_USER("create"),
    LOGIN("login"),
    LOGOUT("logout"),
    POST("post"),
    ADD_FOLLOWER("add_follower"),
    DELETE_FOLLOWER("delete_follower"),
    READ_UNREAD("read_unread");

    private final String name;
    private static final Map<String, Operation> opMap = new HashMap<String, Operation>();
    private static final Map<String, String> descMap = new HashMap<String, String>();

    static {
        for (Operation op : Operation.values()) {
            opMap.put(op.name, op);
            descMap.put(op.name, "");
        }
    }

    private Operation(String name) {
       this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Set<String> getNames() {
        return opMap.keySet();
    }

    public static Map<String, String> getDescriptionMap() {
        return Collections.unmodifiableMap(descMap);
    }

    public static Operation fromString(String name) {
        if (opMap.containsKey(name)) {
            return opMap.get(name);
        }
        throw new NoSuchElementException(name + "not found");
    }
}
