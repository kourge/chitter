import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class SimpleMemoryFS implements SimpleFS {
    private Map<String, byte[]> map;

    public SimpleMemoryFS() {
        this.map = new HashMap<String, byte[]>();
    }

    public String toString() {
        return String.format("<SimpleMemoryFS %s>", this.map.toString());
    }

    public boolean create(String filename) {
        if (this.exists(filename)) {
            return false;
        }

        this.map.put(filename, new byte[0]);
        return true;
    }

    public boolean exists(String filename) {
        return this.map.containsKey(filename);
    }

    public byte[] read(String filename) {
        if (!this.exists(filename)) {
            return null;
        }

        byte[] data = this.map.get(filename);
        return Arrays.copyOf(data, data.length);
    }

    public boolean append(String filename, byte[] data) {
        if (!this.exists(filename)) {
            return false;
        }

        byte[] current = this.map.get(filename);
        byte[] newData = Arrays.copyOf(current, current.length + data.length);
        System.arraycopy(data, 0, newData, current.length, data.length);
        this.map.put(filename, newData);
        return true;
    }

    public boolean overwrite(String filename, byte[] data) {
        if (!this.exists(filename)) {
            return false;
        }

        this.map.put(filename, Arrays.copyOf(data, data.length));
        return true;
    }

    public boolean delete(String filename) {
        if (!this.exists(filename)) {
            return false;
        }

        this.map.remove(filename);
        return true;
    }
}
