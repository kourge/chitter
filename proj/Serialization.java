import java.io.*;

public class Serialization {
    private Serialization() {}

    public static class EncodingException extends Exception {
        public EncodingException(String msg) { super(msg); }
        public EncodingException(String msg, Throwable cause) { super(msg, cause); }
        public EncodingException(Throwable cause) { super(cause); }
    }

    public static class DecodingException extends Exception {
        public DecodingException(String msg) { super(msg); }
        public DecodingException(String msg, Throwable cause) { super(msg, cause); }
        public DecodingException(Throwable cause) { super(cause); }
    }

    public static byte[] encode(Object obj) throws EncodingException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new EncodingException(e);
        }
    }

    public static Object decode(byte[] buffer) throws DecodingException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            ObjectInputStream ios = new ObjectInputStream(bais);
            Object obj = ios.readObject();
            ios.close();
            return obj;
        } catch (IOException e) {
            throw new DecodingException(e);
        } catch (ClassNotFoundException e) {
            throw new DecodingException(e);
        }
    }
}
