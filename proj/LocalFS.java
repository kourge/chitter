import java.io.*;

import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.PersistentStorageInputStream;
import edu.washington.cs.cse490h.lib.PersistentStorageOutputStream;
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

        PersistentStorageInputStream reader;
        try {
            reader = node.getInputStream(filename);
        } catch (FileNotFoundException e) {
            return EMPTY_RESULT;
        }

        try {
            ByteArrayOutputStream readBytes = new ByteArrayOutputStream();
            byte[] buf = new byte[1024]; // try to read 1024 bytes at a time
            int offs = 0;
            while (true) {
                int amountRead = reader.read(buf, offs, 1024);
                readBytes.write(buf, offs, amountRead);
                offs += amountRead;
                // stop when we can read nothing more
                if (amountRead == 0) {
                    break;
                }
            }
            out = readBytes.toByteArray();
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
            PersistentStorageOutputStream writer = node.getOutputStream(filename, true);
            writer.write(data);
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
            PersistentStorageOutputStream writer = node.getOutputStream(filename, false);
            writer.write(data);
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
