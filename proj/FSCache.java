import java.util.*;

/** A simple in-memory cache of remote filesystem objects */
public class FSCache {

    private Map<String, Pair<Long, byte[]>> cache;

    public FSCache() {
        this.cache = new HashMap<String, Pair<Long, byte[]>>();
    }

    public Pair<Long, byte[]> get(String name) {
        return this.cache.get(name);
    }

    public void remove(String name) {
        this.cache.remove(name);
    }

    public void put(String name, long ver, byte[] data) {
        this.put(name, Pair.of(ver, data));
    }

    public void put(String name, Pair<Long, byte[]> file) {
        this.cache.put(name, file);
    }
}
