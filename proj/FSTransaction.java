import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
import edu.washington.cs.cse490h.lib.Utility;

/**
 * FSTransaction facilitates meaningful file system related behavior for
 * transactions. It performs error detection and rolls back if necessary.
 *
 * Error detection is done by examining the return value after each invocation.
 * If the return value is considered a failure value for a given invocation, the
 * transaction aborts and begins to roll back any changes made.
 *
 * Rolling back is implemented through snapshots. A snapshot is created from
 * a delta, which is a list of all filenames touched by mutative invocations in
 * a transaction. Each file that will be mutated is then copied under a mangled
 * snapshot filename, and this correspondance is stored in a map.
 *
 * Before the transaction begins, a snapshot is taken. All transactions in the
 * transaction are then rewritten to target the snapshot version of the files
 * instead of the real files. If an error is detected throughout the
 * transaction, then the snapshot is deleted and never committed. If the
 * transaction is successful, then the snapshot will be committed.
 *
 * Committing a snapshot involves overwriting the real files with their snapshot
 * equivalents. This process is logged, so if a failure occurs in the middle of
 * committing a snapshot, when the node restarts, the commit continues as part
 * of the recovery process.
 */
public class FSTransaction extends Transaction implements Serializable {
    public static final long serialVersionUID = 0L;

    private transient LocalFS fs;

    public FSTransaction(Transaction transaction, LocalFS fs) {
        super(transaction.getInvocations());
        this.fs = fs;
    }

    // name of the log file we'll use for committing snapshots
    public static final String COMMIT_LOGFILE = ".$snapshot$log";

    private Map<String, Object> failureValues = new HashMap<String, Object>() {{
        put("create", FS.FAILURE);
        put("read", FS.EMPTY_RESULT);
        put("currentVersion", FS.FAILURE);
        put("appendIfNotChanged", FS.FAILURE);
        put("overwriteIfNotChanged", FS.FAILURE);
        put("delete", false);
        put("isSameVersion", false);
    }};

    public Object invokeOn(Object obj) throws InvocationException {
        Map<String, String> snapshot = this.compileSnapshot(this.calls);
        Object result = null;
        try {
            result = super.invokeOn(obj);
        } catch (InvocationException e) {
            this.deleteSnapshot(snapshot);
            throw e;
        }
        this.commitSnapshot(snapshot);
        this.deleteSnapshot(snapshot);
        return result;
    }

    public Object invoke() throws InvocationException {
        Map<String, String> snapshot = this.compileSnapshot(this.calls);
        Object result = null;
        try {
            result = super.invoke();
        } catch (InvocationException e) {
            this.deleteSnapshot(snapshot);
            throw e;
        }
        this.commitSnapshot(snapshot);
        this.deleteSnapshot(snapshot);
        return result;
    }

    private Map<String, String> compileSnapshot(Invocation... ivs) {
        Map<String, String> snapshot = new HashMap<String, String>();

        // loop through and decide who needs a snapshot (any file that will be mutated)
        for (Invocation iv : ivs) {
            String file = (String)iv.getParameterValues()[0];

            if (snapshot.containsKey(file)) {
                continue;
            }

            if (InvokableUtils.mutates(iv.getMethodName())) {
                String snapshotName;
                do {
                    snapshotName = snapshotNameForFile(file);
                } while (fs.exists(snapshotName));

                if (iv.getMethodName().equals("delete")) {
                    // if we're a delete, our "snapshot" is the lack of a file
                    snapshot.put(file, "");
                } else {
                    // there's no file to copy in create case:
                    if (!iv.getMethodName().equals("create")) {
                        this.fs.copy(file, snapshotName);
                    }
                    snapshot.put(file, snapshotName);
                }
            }
        }

        // Now run through and re-target operations that will be acting on a snapshot
        for (Invocation iv : ivs) {
            String file = (String)iv.getParameterValues()[0];
            if (snapshot.containsKey(file) && !iv.getMethodName().equals("isSameVersion")) {
                iv.setParameterValue(0, snapshot.get(file));
            }
        }
        return snapshot;
    }

    private String snapshotNameForFile(String file) {
        return String.format("snap$%d$%s", System.currentTimeMillis(), file);
    }

    /** Copies snapshot-ed files over to the actual files they're replacing */
    private void commitSnapshot(Map<String, String> snapshot) {
        if (snapshot.isEmpty()) {
            return; // must've been read-only
        }

        // log file we'll track these with (if we failed prior to getting here,
        // then no biggie, we've just got some now-orphaned snapshots in our fs)
        this.fs.delete(COMMIT_LOGFILE);
        this.fs.create(COMMIT_LOGFILE);

        // each pair of lines is a snapshot -> final file pair
        for (String file : snapshot.keySet()) {
            String snapshotName = snapshot.get(file);
            this.fs.appendIfNotChanged(COMMIT_LOGFILE, Utility.stringToByteArray(snapshotName + "\n"), -1);
            this.fs.appendIfNotChanged(COMMIT_LOGFILE, Utility.stringToByteArray(file + "\n"), -1);
        }

        // denotes end of snapshot listing
        this.fs.appendIfNotChanged(COMMIT_LOGFILE, Utility.stringToByteArray("/SNAPSHOTS\n"), -1);

        for (String file : snapshot.keySet()) {
            String snapshotName = snapshot.get(file);
            if (snapshotName.equals("")) {
                this.fs.delete(file);
            } else {
                this.fs.copy(snapshotName, file);
            }

            // we finished one commit
            this.fs.appendIfNotChanged(COMMIT_LOGFILE, Utility.stringToByteArray("COMPLETE\n"), -1);
        }
    }

    private void deleteSnapshot(Map<String, String> snapshot) {
        for (String file : snapshot.keySet()) {
            String snapshotName = snapshot.get(file);
            this.fs.delete(snapshotName);
        }
        // if we made it this far, we either failed, or have committed,
        // so we can also kill off the log file
        this.fs.delete(COMMIT_LOGFILE);
    }

    @Override protected void afterInvocation(Invocation iv) throws InvocationException {
        String name = iv.getMethodName();
        if (failureValues.containsKey(name)
            && iv.getReturnValue().equals(failureValues.get(name))) {
            this.failed = true;
            throw new InvocationException("FS operation failed: " + name);
        }
    }
}
