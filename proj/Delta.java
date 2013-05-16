import java.io.*;

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

    public Delta(Type type) {
        this.type = type;
    }

    public Delta(Type type, byte[] data) {
        this(type);
        this.data = data;
    }

    public String toString() {
        if (this.data == null) {
            return String.format("(%s)", this.type.symbol());
        } else {
            return String.format(
                "(%s %d)", this.type.symbol(), this.data.length
            );
        }
    }
}

