import java.io.*;

import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.Utility;

public class LocalFS implements FS {
    private ClientServerNode node;

    public LocalFS(ClientServerNode n) {
        node = n;
    }

    /** Create a file by name, return version number */
    public long create(String filename) {
        if (Utility.fileExists(node, filename)) {
            return Utility.fileTimestamp(node, filename);
        }

        try {
            node.getWriter(filename, false);
            return Utility.fileTimestamp(node, filename);
        } catch (IOException e) {
            return FS.FAILURE;
        }
    }

    /** Gets whether or not a file exists */
    public boolean exists(String filename) {
        boolean ex = Utility.fileExists(node, filename);
        return ex;
    }

    /** Reads a file by name*/
    public Pair<byte[], Long> read(String filename) {
        byte[] out;

        PersistentStorageReader reader;
        try {
            reader = node.getReader(filename);
        } catch (FileNotFoundException e) {
            return EMPTY_RESULT;
        }

        try {
            StringBuffer buf = new StringBuffer();
            String tmp = null;
            while ((tmp = reader.readLine()) != null) {
                buf.append(tmp);
            }
            out = Utility.stringToByteArray(buf.toString());
        } catch (IOException e) {
            return EMPTY_RESULT;
        }

        return Pair.of(out, Utility.fileTimestamp(node, filename));
    }

    /** Gives the latest version number of a file */
    public long currentVersion(String filename) {
        if (!Utility.fileExists(node, filename)) {
            return FS.FAILURE;
        }

        return Utility.fileTimestamp(node, filename);
    }

    /** Append to a file if not changed since version we have */
    public long appendIfNotChanged(String filename, byte[] data, long version) {
        if (!Utility.fileExists(node, filename)) {
            return FS.FAILURE;
        }

        if (version != FS.FAILURE &&
            version != Utility.fileTimestamp(node, filename)) {
            return FS.FAILURE;
        }

        try {
            BufferedWriter writer = node.getWriter(filename, true);
            writer.append(Utility.byteArrayToString(data));
            return Utility.fileTimestamp(node, filename);
        } catch (IOException e) {
            return FS.FAILURE;
        }
    }

    /** Write a file if not changed since the version we have */
    public long overwriteIfNotChanged(String filename, byte[] data, long version) {
        if (!Utility.fileExists(node, filename)) {
            return FS.FAILURE;
        }

        if (version != FS.FAILURE &&
            version != Utility.fileTimestamp(node, filename)) {
            return FS.FAILURE;
        }

        try {
            PersistentStorageWriter writer = node.getWriter(filename, false);
            writer.write(new String(data));
            return Utility.fileTimestamp(node, filename);
        } catch (IOException e) {
            return FS.FAILURE;
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
