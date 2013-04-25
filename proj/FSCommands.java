import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;

import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Node;
import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.PersistentStorageInputStream;
import edu.washington.cs.cse490h.lib.PersistentStorageOutputStream;
import edu.washington.cs.cse490h.lib.Utility;

public class FSCommands {
    private ClientServerNode node;

    public FSCommands(ClientServerNode n) {
        node = n;
    }

    /** Create a file by name, return version number */
    public long create(String filename) {
        if (Utility.fileExists(node, filename)) {
            return Utility.fileTimestamp(node, filename);
        }

        try {
            node.getWriter(filename, false);
            System.out.println("created: " + filename);
            return Utility.fileTimestamp(node, filename);
        } catch (IOException e) {
            return -1;
        }
    }

    /** Gets whether or not a file exists */
    public boolean exists(String filename) {
        boolean ex = Utility.fileExists(node, filename);
        System.out.println("checking: " + filename + " exists: " + ex);
        return ex;
    }

    /** Reads a file by name*/
    public Pair<byte[], Long> read(String filename) {
        byte[] out;

        PersistentStorageReader reader;
        try {
            reader = node.getReader(filename);
        } catch (FileNotFoundException e) {
            return null;
        }

        try {
            StringBuffer buf = new StringBuffer();
            String tmp = null;
            while ((tmp = reader.readLine()) != null) {
                buf.append(tmp);
            }
            System.out.println("Read: " + buf.toString());
            out = buf.toString().getBytes();
        } catch (IOException e) {
            return null;
        }

        return Pair.of(out, Utility.fileTimestamp(node, filename));
    }

    /** Gives the latest version number of a file */
    public long currentVersion(String filename) {
        if (!Utility.fileExists(node, filename)) {
            return -1;
        }

        return Utility.fileTimestamp(node, filename);
    }

    /** Append to a file if not changed since version we have */
    public long appendIfNotChanged(String filename, byte[] data, long version) {
        if (!Utility.fileExists(node, filename)) {
            return -1;
        }

        if (version != -1 && version != Utility.fileTimestamp(node, filename)) {
            return -1;
        }

        try {
            BufferedWriter writer = node.getWriter(filename, true);
            writer.append(new String(data));
            return Utility.fileTimestamp(node, filename);
        } catch (IOException e) {
            return -1;
        }
    }

    /** Write a file if not changed since the version we have */
    public long overwriteIfNotChanged(String filename, byte[] data, long version) {
        if (!Utility.fileExists(node, filename)) {
            return -1;
        }

        if (version != -1 && version != Utility.fileTimestamp(node, filename)) {
            return -1;
        }

        try {
            PersistentStorageWriter writer = node.getWriter(filename, false);
            writer.write(new String(data));
            return Utility.fileTimestamp(node, filename);
        } catch (IOException e) {
            return -1;
        }
    }

    /** Gets whether or not the file has changed from the version we have */
    public boolean hasChanged(String filename, long version) {
        return version != Utility.fileTimestamp(node, filename);
    }

    /** Delete a file by name */
    public boolean delete(String filename) {
        try {
            PersistentStorageWriter writer = node.getWriter(filename, false);
            writer.delete();
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
