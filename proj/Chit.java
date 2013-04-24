import java.io.*;

public class Chit implements Serializable {
    public static final long serialVersionUID = 0L;

    private String text;
    private long timestamp;

    public Chit(String text, long timestamp) {
        this.text = text;
        this.timestamp = timestamp;
    }

    public Chit(String text) {
        this(text, System.currentTimeMillis());
    }

    public String getText() { return this.text; }
    public long getTimestamp() { return this.timestamp; }
}
