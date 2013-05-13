import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

public class FSTransaction extends Transaction {
    private LocalFS fs;

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
        return result;
    }

    private Map<String, String> compileSnapshot(Invocation... ivs) {
        return this.createSnapshot(ivs);
    }

    private Map<String, String> createSnapshot(Invocation... ivs) {
        Map<String, String> snapshot = new HashMap<String, String>();
        for (Invocation iv : ivs) {
            String file = (String)iv.getParameterValues()[0];
            if (snapshot.containsKey(file)) {
                (String)iv.getParameterValues()[0] = snapshot.get(file);
                continue;
            }

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
            // redirect operation to apply to snapshot instead of real version
            (String)iv.getParameterValues()[0] = snapshot.get(file);
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

    protected void beforeInvocation(Invocation iv) throws InvocationException {}

    protected void afterInvocation(Invocation iv) throws InvocationException {
        String name = iv.getMethodName();
        if (failureValues.containsKey(name) 
            && iv.getReturnValue().equals(failureValues.get(name))) {
            throw new InvocationException(new Exception());
        }
    }
}
