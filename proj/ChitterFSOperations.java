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

public class ChitterFSOperations {

    private ChitterNode node;

    public ChitterFSOperations(ChitterNode n) {
        node = n;
    }

    //-------------------------------------
    // basic operations offered via RPC:

    /** Create a file by name, return version number */
    public long create(String filename) {
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
        System.out.println("checking: " + filename);
        return Utility.fileExists(node, filename);
    }

    /** Reads a file by name*/
    public Pair<byte[], Long> read(String filename) {
        // TODO
        /*byte[] out;
        PersistentStorageInputStream reader;
        try {
            reader = node.getInputStream(filename);
        } catch (FileNotFoundException e) {
            return null;
        }
        try {
            reader.read(out);
        } catch (IOException e) {
            return null;
        }
        return Pair.of(out, Utility.fileTimestamp(node, filename));*/
        return null;
    }

    /** Append to a file if not changed since version we have */
    public long appendIfNotChanged(String filename, byte[] data, long version) {
        // TODO
        /*if (!Utility.fileExists(node, filename)) {
            return -1;
        }
        try {
            node.getWriter(filename, true);
            node.append(data);
            return Utility.fileTimestamp(node, filename);
        } catch (IOException e) {
            return -1;
        }*/
        return -1;
    }

    /** Write a file if not changed since the version we have */
    public long overwriteIfNotChanged(String filename, byte[] data, long version) {
        // TODO
        /*if (!Utility.fileExists(node, filename)) {
            return -1;
        }
        try {
            PersistentStorageWriter writer = node.getWriter(filename, false);
            writer.write(data);
            return Utility.fileTimestamp(node, filename);
        } catch (IOException e) {
            return -1;
        }*/
        return -1;
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

    //
    //-------------------------------------
}
