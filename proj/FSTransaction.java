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

    public Object invokeOn(Object obj) throws InvocationException {
        Map<String, String> snapshot = this.compileSnapshot(this.calls);
        Object result = null;
        try {
            result = super.invokeOn(obj);
        } catch (InvocationException e) {
            this.restoreSnapshot(snapshot);
            this.deleteSnapshot(snapshot);
            throw e;
        }
        return result;
    }

    public Object invoke() throws InvocationException {
        Map<String, String> snapshot = this.compileSnapshot(this.calls);
        Object result = null;
        try {
            result = super.invoke();
        } catch (InvocationException e) {
            this.restoreSnapshot(snapshot);
            this.deleteSnapshot(snapshot);
            throw e;
        }
        return result;
    }

    private Map<String, String> compileSnapshot(Invocation... ivs) {
        return this.createSnapshot(this.compileDelta(ivs));
    }

    private Set<String> compileDelta(Invocation... ivs) {
        Set<String> result = new HashSet<String>();
        for (Invocation iv : ivs) {
           result.add((String)iv.getParameterValues()[0]);
        }
        return result;
    }

    private Map<String, String> createSnapshot(Set<String> files) {
        Map<String, String> snapshot = new HashMap<String, String>();
        for (String file : files) {
            String snapshotName;
            do {
                snapshotName = snapshotNameForFile(file);
            } while (fs.exists(snapshotName));

            this.fs.copy(file, snapshotName);
            snapshot.put(file, snapshotName);
        }
        return snapshot;
    }

    private String snapshotNameForFile(String file) {
        return String.format("snap$%d$%s", System.currentTimeMillis(), file);
    }

    private void restoreSnapshot(Map<String, String> snapshot) {
        for (String file : snapshot.keySet()) {
            String snapshotName = snapshot.get(file);
            this.fs.copy(snapshotName, file);
        }
    }

    private void deleteSnapshot(Map<String, String> snapshot) {
        for (String file : snapshot.keySet()) {
            String snapshotName = snapshot.get(file);
            this.fs.delete(snapshotName);
        }
    }
}
