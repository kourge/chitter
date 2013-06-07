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

/** A general interface for logging and ensuring that operations get completed
 *      in the presence of failures */
public abstract class Journal {

    private String filename;
    private static final String COMPLETE_TOKEN = "COMPLETE";
    private PersistentStorageWriter log;
    protected Node node;

    private Queue<Serializable> pendingOps;

    public Journal(String filename, Node node) throws JournalException {
        this.filename = filename;
        this.node = node;
        this.pendingOps = new LinkedList<Serializable>();
        recover();
    }
    
    /** Serialize and base64-encode an object into our file
     *  @return Whether we succeeded */
    public boolean push(Serializable obj) throws JournalException {
        String b64;
        try {
            byte[] out = Serialization.encode(obj);
            b64 = Utility.toBase64(out);
        } catch (Serialization.EncodingException e) {
            return false;
        }

        this.pendingOps.offer(obj);
        try {
            log.write(b64 + "\n");
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /** Do the next pending operation */
    public void pop() throws JournalException {
        if (!this.pendingOps.isEmpty()) {
            Serializable op = this.pendingOps.poll();
            execute(op);
            complete();
        } else {
            throw new JournalException("Journal underflow.");
        }
    }

    /** Run through and finish all pending operations */
    public void completePendingOps() throws JournalException {
        while (!this.pendingOps.isEmpty()) {
            Serializable op = this.pendingOps.poll();
            execute(op);
            complete();
        }
    }

    /** Mark an operation complete */
    public void complete() throws JournalException {
        try {
        // NOTE: The extending class should be able to handle the case of
        // a failure before we can write this complete marker, logging a
        // file version is probably the sanest way of doing so (if the version
        // of the file we were modifying has changed from the journal entry
        // we must've completed the op, but not written to the log).
            log.write(COMPLETE_TOKEN + "\n");
        } catch (IOException e) {
            throw new JournalException("Failed to write completion to journal.");
        }

        // garbage collect
        if (pendingOps.isEmpty()) {
            // we want to empty the file, but keep it around
            try {
                this.log = this.node.getWriter(this.filename, false);
            } catch (IOException e) {
                throw new JournalException("Failed to garbage collect log file.");
            }
        }
    }
    
    /** Recover from a failure */
    private void recover() throws JournalException {
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
                        if (line.equals(COMPLETE_TOKEN)) {
                            this.pendingOps.poll();
                        } else {
                            Serializable obj = Utility.fromBase64(line);
                            this.pendingOps.offer(obj);
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

    /** Execute a pending operation */
    public abstract void execute(Serializable obj);
}

