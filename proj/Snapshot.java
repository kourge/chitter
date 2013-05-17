import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.io.*;
import edu.washington.cs.cse490h.lib.Utility;

public class Snapshot implements TransactionalFS, Serializable {
    public static final long serialVersionUID = 0L;

    public FS fs;
    private long id;
    private Map<String, Delta> deltas;

    public Snapshot(FS fs) {
        this.fs = fs;
        this.id = Utility.getRNG().nextLong();
        this.deltas = new HashMap<String, Delta>();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Snapshot)) {
            return false;
        }
        Snapshot other = (Snapshot)obj;
        return this.id == other.id && this.deltas.equals(other.deltas);
    }

    public String toString() {
        return String.format("<Snapshot %x %s>", this.id, this.deltas);
    }

    public Map<String, Delta> getDeltas() {
        return Collections.unmodifiableMap(this.deltas);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeLong(id);
        for (String filename : this.deltas.keySet()) {
            Delta d = this.deltas.get(filename);
            oos.writeUTF(filename);
            d.writeObject(oos);
        }
    }

    private void readObject(ObjectInputStream ois)
    throws IOException, ClassNotFoundException {
        this.deltas = new HashMap<String, Delta>();
        this.id = ois.readLong();

        try {
            while (true) {
                String filename = ois.readUTF();
                Delta d = new Delta();
                d.readObject(ois);
                this.deltas.put(filename, d);
            }
        } catch (IOException e) {}
    }

    private void readObjectNoData() throws ObjectStreamException {}

    public boolean create(String filename) {
        if (this.deltas.containsKey(filename)) {
            // The delta layer already has an overlay for this file.
            Delta d = this.deltas.get(filename);

            // The file has been virtually overwritten or appended. It cannot be
            // virtually created until it has been virtually deleted.
            if (d.type != Delta.Type.DELETE) {
                return false;
            }

            // The file has been virtually deleted. Virtually create the file.
            if (d.type == Delta.Type.DELETE) {
                d.type = Delta.Type.OVERWRITE;
                d.data = new byte[0];
            }
        } else {
            // We attempt to make an overlay for this file.

            // The file already physically exists. It cannot be virtually
            // created until it has been virtually deleted.
            if (this.fs.exists(filename)) {
                return false;
            }

            // The file does not physically exist. Virtually create the file.
            Delta d = new Delta(Delta.Type.OVERWRITE, new byte[0]);
            this.deltas.put(filename, d);
        }
        return true;
    }

    public boolean exists(String filename) {
        if (this.deltas.containsKey(filename)) {
            // The delta layer already has an overlay for this file. The file is
            // considered to exist unless the file has been virtually deleted.
            return this.deltas.get(filename).type != Delta.Type.DELETE;
        }

        // This file has not been virtually modified. Delegate the query of its
        // actual existence to the storage layer.
        return this.fs.exists(filename);
    }

    public byte[] read(String filename) {
        if (!this.exists(filename)) {
            return null;
        }

        if (this.deltas.containsKey(filename)) {
            // The file has been virtually modified.
            Delta d = this.deltas.get(filename);

            switch (d.type) {
            case OVERWRITE:
                // If the file has been virtually overwritten, then the
                // virtual content takes precedence over any data that may be in
                // the storage layer.
                return Arrays.copyOf(d.data, d.data.length);
            case APPEND:
                byte[] existingData = this.fs.read(filename).first();
                byte[] overlayData = d.data;
                byte[] data = Arrays.copyOf(
                    existingData, existingData.length + overlayData.length
                );
                System.arraycopy(
                    overlayData, 0,
                    data, existingData.length, overlayData.length
                );
                return data;
            }
        } else {
            // The file has not been virtually modified. Delegate the entire
            // read of this file to the storage layer.
            return this.fs.read(filename).first();
        }

        // This should not be reached.
        return null;
    }

    public boolean append(String filename, byte[] data) {
        if (!this.exists(filename)) {
            return false;
        }

        if (this.deltas.containsKey(filename)) {
            Delta d = this.deltas.get(filename);

            // The delta type is either an overwrite or an append. If it were an
            // overwrite, we merge the overwrite with this append, resulting in
            // a bigger overwrite. If it were an append, the same merge
            // operation results in a bigger append.
            int oldLength = d.data.length;
            d.data = Arrays.copyOf(d.data, oldLength + data.length);
            System.arraycopy(data, 0, d.data, oldLength, data.length);
        } else {
            Delta d = new Delta(
                Delta.Type.APPEND,
                Arrays.copyOf(data, data.length)
            );
            this.deltas.put(filename, d);
        }

        return true;
    }

    public boolean overwrite(String filename, byte[] data) {
        if (!this.exists(filename)) {
            return false;
        }

        if (this.deltas.containsKey(filename)) {
            Delta d = this.deltas.get(filename);
            d.type = Delta.Type.OVERWRITE;
            d.data = Arrays.copyOf(data, data.length);
        } else {
            Delta d = new Delta(
                Delta.Type.OVERWRITE,
                Arrays.copyOf(data, data.length)
            );
            this.deltas.put(filename, d);
        }
        return true;
    }

    public boolean delete(String filename) {
        if (!this.exists(filename)) {
            return false;
        }

        if (this.deltas.containsKey(filename)) {
            Delta d = this.deltas.get(filename);
            d.type = Delta.Type.DELETE;
            d.data = new byte[0];
        } else {
            Delta d = new Delta(Delta.Type.DELETE);
            this.deltas.put(filename, d);
        }
        return true;
    }

    public void commit() {
        for (String filename : this.deltas.keySet()) {
            Delta d = this.deltas.get(filename);
            switch (d.type) {
            case DELETE:
                this.fs.delete(filename);
                break;
            case OVERWRITE:
                if (!this.fs.exists(filename)) {
                    this.fs.create(filename);
                }
                this.fs.overwriteIfNotChanged(filename, d.data, -1);
                break;
            case APPEND:
                this.fs.appendIfNotChanged(filename, d.data, -1);
                break;
            }
        }
    }
}
