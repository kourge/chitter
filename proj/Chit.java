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

    public String toString() {
        return String.format("<Chit time=%d text=\"%s\">", this.timestamp, this.text);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeUTF(String.format("%d\t%s", this.timestamp, this.text));
    }

    private void readObject(ObjectInputStream ois)
    throws IOException, ClassNotFoundException {
        String line = ois.readUTF();
        int delimiter = line.indexOf("\t");

        this.timestamp = Long.parseLong(line.substring(0, delimiter));
        this.text = line.substring(delimiter + 1);
    }

    private void readObjectNoData() throws ObjectStreamException {}
}
