package edu.washington.cs.cse490h.lib;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import javax.xml.bind.DatatypeConverter;

/**
 * Provides some useful static methods.
 */
public class Utility {

    private static final String CHARSET = "UTF-8";
    static Random randNumGen;

    public static Random getRNG() {
        return randNumGen;
    }

    public static void setRNG(Random r) {
        randNumGen = r;
    }

    /**
     * Convert a string to a byte[]
     *
     * @param msg
     *            The string to convert
     * @return The byte[] that the string was converted to
     */
    public static byte[] stringToByteArray(String msg) {
        try {
            return msg.getBytes(CHARSET);
        } catch (UnsupportedEncodingException e) {
            System.err
                    .println("Exception occured while converting string to byte array. String: "
                            + msg + " Exception: " + e);
        }
        return null;
    }

    /**
     * Convert a byte[] to a string
     *
     * @param msg
     *            The byte[] to convert
     * @return The converted String
     */
    public static String byteArrayToString(byte[] msg) {
        try {
            return new String(msg, CHARSET);
        } catch (UnsupportedEncodingException e) {
            System.err
                    .println("Exception occured while converting byte array to string. Exception: "
                            + e);
        }
        return null;
    }

    /**
     * Escapes a string to be suitable for inclusion on a synoptic log event
     * line.
     *
     * @param s
     *            the string to escape
     * @return the escaped string
     */
    public static String logEscape(String s) {
        if (s != null) {
            s = s.replace(" ", "_");
            s = s.replace("\n", "|");
            return "'" + s + "'";
        }
        return "''";
    }

    static String realFilename(int nodeAddr, String filename) {
        return "storage/" + nodeAddr + "/" + filename;
    }

    static void mkdirs(int nodeAddr) {
        File f = new File("storage/" + nodeAddr + "");
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    public static boolean fileExists(Node n, String filename) {
        File f = new File(realFilename(n.addr, filename));
        return f.exists();
    }

    public static long fileTimestamp(Node n, String filename) {
        File f = new File(realFilename(n.addr, filename));
        return f.lastModified();
    }

    public static String camelize(String string) {
        StringBuffer b = new StringBuffer();
        b.append(Character.toUpperCase(string.charAt(0)));

        for (int i = 1; i < string.length(); i++) {
            char c = string.charAt(i);

            if (c == '_' || c == '-') {
                b.append(Character.toUpperCase(string.charAt(++i)));
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }

    public static String toBase64(byte[] array) {
        return DatatypeConverter.printBase64Binary(array);
    }

    public static byte[] fromBase64(String value) {
        return DatatypeConverter.parseBase64Binary(value);
    }
}
