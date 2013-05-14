import java.io.*;

public class Chit implements Serializable, Comparable<Chit> {
    public static final long serialVersionUID = 0L;

    private String text;
    private long timestamp;

    public Chit(String text, long timestamp) {
        this.text = text.trim();
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

    public int compareTo(Chit other) {
        return (int)(other.getTimestamp() - this.getTimestamp());
    }

    private String escape(String string) {
        string = string.replace((CharSequence)"\\", (CharSequence)"\\\\");
        string = string.replace((CharSequence)"\n", (CharSequence)"\\n");
        return string;
    }

    private String unescape(String string) {
        string = string.replace((CharSequence)"\\n", (CharSequence)"\n");
        string = string.replace((CharSequence)"\\\\", (CharSequence)"\\");
        return string;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeUTF(String.format("%d\t%s", this.timestamp, escape(this.text)));
    }

    private void readObject(ObjectInputStream ois)
    throws IOException, ClassNotFoundException {
        String line = ois.readUTF();
        int delimiter = line.indexOf("\t");

        this.timestamp = Long.parseLong(line.substring(0, delimiter));
        this.text = unescape(line.substring(delimiter + 1));
    }

    private void readObjectNoData() throws ObjectStreamException {}
}
