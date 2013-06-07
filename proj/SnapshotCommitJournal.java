import java.io.*;
import java.util.*;
import edu.washington.cs.cse490h.lib.PersistentStorageReader;
import edu.washington.cs.cse490h.lib.PersistentStorageWriter;
import edu.washington.cs.cse490h.lib.PersistentStorageInputStream;
import edu.washington.cs.cse490h.lib.PersistentStorageOutputStream;
import edu.washington.cs.cse490h.lib.Utility;
import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Node;

/** Journal for snapshot commits */
public class SnapshotCommitJournal extends Journal {

    FS fs;
    
    public SnapshotCommitJournal(Node n, FS fs) throws JournalException {
        super("$snapshot_commit_log", n);
        this.fs = fs;
    }

    public void addDelta(String filename, Delta d) throws JournalException {
        push(new DeltaEntry(filename, d, Utility.fileTimestamp(this.node, filename)));
    }

    @Override public void execute(Serializable s) {
        DeltaEntry d = (DeltaEntry)s;
        switch (d.delta.type) {
        case DELETE:
            if (Utility.fileExists(this.node, d.filename)) {
                this.fs.delete(d.filename);
            }
            break;
        case OVERWRITE:
            if (!this.fs.exists(d.filename)) {
                this.fs.create(d.filename);
            }
            this.fs.overwriteIfNotChanged(d.filename, d.delta.data, -1);
            break;
        case APPEND:
            // We already did the append if the version number has been bumped
            if (Utility.fileTimestamp(this.node, d.filename) == d.version) {
                this.fs.appendIfNotChanged(d.filename, d.delta.data, -1);
            }
            break;
        }
    }
}

class DeltaEntry implements Serializable {
    public static final long serialVersionUID = 0L;
    public String filename;
    public Delta delta;
    public long version;
    public DeltaEntry(String filename, Delta delta, long version) {
        this.filename = filename;
        this.delta = delta;
        this.version = version;
    }
};
