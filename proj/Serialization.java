import java.io.*;

public class Serialization {
    public static byte[] encode(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    public static Object decode(byte[] buffer)
    throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        ObjectInputStream ios = new ObjectInputStream(bais);
        Object obj = ios.readObject();
        ios.close();
        return obj;
    }
}
