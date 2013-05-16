import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * An implementation of the FS interface that entirely exists in memory.
 */
public class MemoryFS implements FS {
    protected class File {
        public long timestamp;
        public byte[] content;

        public File() {
            this.timestamp = System.currentTimeMillis();
            this.content = new byte[0];
        }

        public String toString() {
            return String.format(
                "<%x %s>", this.timestamp, Arrays.toString(this.content)
            );
        }
    }

    protected Map<String, File> map;

    public MemoryFS() {
        this.map = new HashMap<String, File>();
    }

    public String toString() {
        return String.format("<MemoryFS %s>", this.map.toString());
    }

    public long create(String filename) {
        if (this.exists(filename)) {
            return this.map.get(filename).timestamp;
        }

        File f = new File();
        this.map.put(filename, f);
        return f.timestamp;
    }

    public boolean exists(String filename) {
        return this.map.containsKey(filename);
    }

    public Pair<byte[], Long> read(String filename) {
        if (!this.exists(filename)) {
            return FS.EMPTY_RESULT;
        }

        File f = this.map.get(filename);
        return Pair.of(Arrays.copyOf(f.content, f.content.length), f.timestamp);
    }

    public long currentVersion(String filename) {
        if (!this.exists(filename)) {
            return FS.FAILURE;
        }

        File f = this.map.get(filename);
        return f.timestamp;
    }

    public long appendIfNotChanged(String filename, byte[] data, long version) {
        if (!this.exists(filename)) {
            return FS.FAILURE;
        }

        File f = this.map.get(filename);
        if (version != FS.FAILURE && f.timestamp != version) {
            return FS.FAILURE;
        }

        int oldLength = f.content.length;
        f.content = Arrays.copyOf(f.content, oldLength + data.length);
        System.arraycopy(data, 0, f.content, oldLength, data.length);

        f.timestamp = System.currentTimeMillis();
        return f.timestamp;
    }

    public long overwriteIfNotChanged(String filename, byte[] data, long version) {
        if (!this.exists(filename)) {
            return FS.FAILURE;
        }

        File f = this.map.get(filename);
        if (version != FS.FAILURE && f.timestamp != version) {
            return FS.FAILURE;
        }

        f.content = Arrays.copyOf(data, data.length);
        f.timestamp = System.currentTimeMillis();
        return f.timestamp;
    }

    public boolean hasChanged(String filename, long version) {
        if (!this.exists(filename)) {
            return false;
        }

        File f = this.map.get(filename);
        return version != f.timestamp;
    }

    public boolean delete(String filename) {
        if (!this.exists(filename)) {
            return false;
        }

        this.map.remove(filename);
        return true;
    }

    public boolean isSameVersion(String filename, long version) {
        if (!this.exists(filename)) {
            return false;
        }

        File f = this.map.get(filename);
        return version != f.timestamp;
    }
}
