import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

public class FSTransaction extends Transaction implements Serializable {
    public static final long serialVersionUID = 0L;

    private transient LocalFS fs;

    public FSTransaction(Transaction transaction, LocalFS fs) {
        super(transaction.getInvocations());
        this.fs = fs;
    }

    private Map<String, Object> failureValues = new HashMap<String, Object>() {{
        put("create", FS.FAILURE);
        put("read", FS.EMPTY_RESULT);
        put("currentVersion", FS.FAILURE);
        put("appendIfNotChanged", FS.FAILURE);
        put("overwriteIfNotChanged", FS.FAILURE);
        put("delete", false);
    }};

    private Map<String, Boolean> mutates = new HashMap<String, Boolean>() {{
        put("create", true);
        put("read", false);
        put("currentVersion", false);
        put("appendIfNotChanged", true);
        put("overwriteIfNotChanged", true);
        put("delete", true);
        put("hasChanged", false);
        put("exists", false);
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

            if (mutates.get(iv.getMethodName())) {
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
            if (snapshot.containsKey(file)) {
                iv.setParameterValues(0, snapshot.get(file));
            }
        }
        return snapshot;
    }

    private String snapshotNameForFile(String file) {
        return String.format("snap$%d$%s", System.currentTimeMillis(), file);
    }

    /** Copies snapshot-ed files over to the actual files they're replacing */
    private void commitSnapshot(Map<String, String> snapshot) {
        // TODO log this carefully so we can recover gracefully
        for (String file : snapshot.keySet()) {
            String snapshotName = snapshot.get(file);
            if (snapshotName.equals("")) {
                this.fs.delete(file);
            } else {
                this.fs.copy(snapshotName, file);
            }
        }
    }

    private void deleteSnapshot(Map<String, String> snapshot) {
        for (String file : snapshot.keySet()) {
            String snapshotName = snapshot.get(file);
            this.fs.delete(snapshotName);
        }
    }

    @Override protected void afterInvocation(Invocation iv) throws InvocationException {
        String name = iv.getMethodName();
        if (failureValues.containsKey(name)
            && iv.getReturnValue().equals(failureValues.get(name))) {
            throw new InvocationException(new Exception());
        }
    }
}
