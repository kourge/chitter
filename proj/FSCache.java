import java.util.*;

/** A simple in-memory cache of remote filesystem objects */
public class FSCache {

    private static final long TIMEOUT = 10000; // 10 seconds for now

    private class CacheEntry {
        private Pair<byte[], Long> file;
        private long timestamp;

        public CacheEntry(Pair<byte[], Long> file) {
            this.file = file;
            this.timestamp = System.currentTimeMillis();
        }

        public Pair<byte[], Long> getFile() {
            return file;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    private Map<String, CacheEntry> cache;

    public FSCache() {
        this.cache = new HashMap<String, CacheEntry>();
    }

    public Pair<byte[], Long> get(String name) {
        CacheEntry e = this.cache.get(name);
        if (e == null) {
            return null;
        } else {
            if (System.currentTimeMillis() - e.getTimestamp() > TIMEOUT) {
                cache.remove(name);
                return null;
            } else {
                return e.getFile();
            }
        }
    }

    public void remove(String name) {
        this.cache.remove(name);
    }

    public void put(String name, long ver, byte[] data) {
        this.put(name, Pair.of(data, ver));
    }

    public void put(String name, Pair<byte[], Long> file) {
        this.cache.put(name, new CacheEntry(file));
    }

    public void invalidateAll() {
        this.cache.clear();
    }
}
