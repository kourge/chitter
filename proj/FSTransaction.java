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
        return super.invokeOn(obj);
    }

    public Object invoke() throws InvocationException {
        return super.invoke();
    }
}
