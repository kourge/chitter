import java.io.*;
import java.util.*;
import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.PersistentStorageInputStream;
import edu.washington.cs.cse490h.lib.PersistentStorageOutputStream;
import edu.washington.cs.cse490h.lib.Utility;
import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Node;
import javax.xml.bind.DatatypeConverter;

/** Journal used to log and complete client operations, 
 *      This is a hack of Journal that uses a Set instead 
 *      of a queue... */
public class ClientJournal {

    Set<String> commands;
    private String filename;
    private static final String COMPLETE_TOKEN = "COMPLETE";
    private PersistentStorageWriter log;
    protected Node node;

    public ClientJournal(String filename, Node n) throws JournalException {
        this.filename = filename;
        this.node = n;
        this.commands = new HashSet<String>();
        recover();
    }

    public boolean push(String command) throws JournalException {
        String b64 = base64Encode(command);
        System.out.println("Logging " + command + " " + b64);
        try {
            log.write(b64 + "\n");
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void complete(String command) throws JournalException {
        try {
            log.write(COMPLETE_TOKEN + base64Encode(command) + "\n");
        } catch (IOException e) {
            throw new JournalException("Failed to write completion to journal.");
        }

        this.commands.remove(command);

        // garbage collect
        if (commands.isEmpty()) {
            // we want to empty the file, but keep it around
            try {
                this.log = this.node.getWriter(this.filename, false);
            } catch (IOException e) {
                throw new JournalException("Failed to garbage collect log file.");
            }
        }
    }

    protected void recover() throws JournalException {
        if (!Utility.fileExists(this.node, this.filename)) {
            // no log file to recover, just start fresh
            startFresh();
        } else {
            PersistentStorageReader reader = null;
            try {
                reader = node.getReader(this.filename);
            } catch (IOException e) {
                throw new JournalException("Failed to open log file: " 
                    + this.filename + " for recovery.");
            }

            if (reader != null) {
                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        if (line.substring(0, COMPLETE_TOKEN.length()).equals(COMPLETE_TOKEN)) {
                            String command = (String)base64Decode(line.substring(COMPLETE_TOKEN.length()));
                            this.commands.remove(command);
                        } else {
                            String command = (String)base64Decode(line);
                            this.commands.add(command);
                        }
                    }
                } catch (IOException e) {
                    throw new JournalException("Error while reading from recovery log.");
                }
            } else {
                throw new JournalException("Failed to open log file: "
                    + this.filename + " for recovery.");
            }
            try {
                this.log = this.node.getWriter(this.filename, true);
            } catch (IOException e) {
                throw new JournalException("Failed to open log file.");
            }
        }
    }

    private void startFresh() throws JournalException {
        // create a fresh log
        try {
            this.log = this.node.getWriter(this.filename, false);
        } catch (IOException e) {
            throw new JournalException("Failed to create initial log file.");
        }
    }

    public Set<String> getCommands() {
        return commands;
    }

    /** TODO these should probably be moved to Utility: */

    /** Get a base64 representation of the serialized version of
     *  the passed-in object */
    private String base64Encode(Serializable obj) throws JournalException {
        byte[] bytes = null;
        try {
            bytes = Serialization.encode(obj);
        } catch(Serialization.EncodingException e) {
            throw new JournalException("Failed base64 encoding: " + e);
        }
        return DatatypeConverter.printBase64Binary(bytes);
    }

    /** Decode a base64 serialized object back into a proper object */
    private Serializable base64Decode(String b64) throws JournalException {
        byte[] bytes = DatatypeConverter.parseBase64Binary(b64);
        Serializable out = null;
        try {
            out = (Serializable)Serialization.decode(bytes);
        } catch(Serialization.DecodingException e) {
            throw new JournalException("Failed base64 decoding.");
        }
        return out;
    }
}

