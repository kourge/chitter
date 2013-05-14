import java.util.*;

/** A simple in-memory cache of remote filesystem objects */
public class FSCache {

    private Map<String, Pair<byte[], Long>> cache;

    public FSCache() {
        this.cache = new HashMap<String, Pair<byte[], Long>>();
    }

    public Pair<byte[], Long> get(String name) {
        return this.cache.get(name);
    }

    public void remove(String name) {
        this.cache.remove(name);
    }

    public void put(String name, long ver, byte[] data) {
        this.put(name, Pair.of(data, ver));
    }

    public void put(String name, Pair<byte[], Long> file) {
        this.cache.put(name, file);
    }

    public void invalidateAll() {
        this.cache.clear();
    }
}
