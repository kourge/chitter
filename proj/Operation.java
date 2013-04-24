import java.util.Map;
import java.util.HashMap;
import java.util.Set;
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
    private static final Map<String, Operation> map = new HashMap<String, Operation>();

    static {
        for (Operation op : Operation.values()) {
            map.put(op.name, op);
        }
    }

    private Operation(String name) {
       this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Set<String> getNames() {
        return map.keySet();
    }

    public static Operation fromString(String name) {
        if (map.containsKey(name)) {
            return map.get(name);
        }
        throw new NoSuchElementException(name + "not found");
    }
}
