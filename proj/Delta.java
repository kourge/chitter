import java.io.*;
import java.util.Arrays;
import edu.washington.cs.cse490h.lib.Utility;

public class Delta implements Serializable {
    public static final long serialVersionUID = 0L;

    public enum Type {
        APPEND("+"), OVERWRITE("="), DELETE("-");
        private final String symbol;
        Type(String symbol) { this.symbol = symbol; }
        public String symbol() { return this.symbol; }
    }

    public Type type;
    public byte[] data;

    public Delta() {}

    public Delta(Type type) {
        this(type, new byte[0]);
    }

    public Delta(Type type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Delta)) {
            return false;
        }
        Delta other = (Delta)obj;
        return this.type == other.type && Arrays.equals(this.data, other.data);
    }

    public String toString() {
        if (this.type == Type.DELETE) {
            return String.format("(%s)", this.type.symbol());
        } else {
            return String.format(
                "(%s %d)", this.type.symbol(), this.data.length
            );
        }
    }

    public void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeUTF(this.type.name());
        oos.writeUTF(Utility.toBase64(data));
    }

    public void readObject(ObjectInputStream ois)
    throws IOException, ClassNotFoundException {
        this.type = Enum.valueOf(Type.class, ois.readUTF());
        this.data = Utility.fromBase64(ois.readUTF());
    }

    public void readObjectNoData() throws ObjectStreamException {}
}

