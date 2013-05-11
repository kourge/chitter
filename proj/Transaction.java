import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.io.Serializable;

public class Transaction
implements Serializable, Invokable, Iterable<Invocation> {
    public static final long serialVersionUID = 0L;

    protected List<Invocation> calls;

    public Transaction(Invocation... args) {
        this.calls = new LinkedList<Invocation>();
        for (Invocation iv : args) {
            this.calls.add(iv);
        }
    }

    public boolean equalsIgnoreValues(Invokable obj) {
        if (!(obj instanceof Transaction)) {
            return false;
        }
        Transaction other = (Transaction)obj;

        return this.calls.equals(other.calls);
    }

    public Object invokeOn(Object obj) throws InvocationException {
        List<Object> result = new LinkedList<Object>();
        for (Invocation iv : this.calls) {
            result.add(iv.invokeOn(obj));
        }
        return result;
    }

    public Object invoke() throws InvocationException {
        List<Object> result = new LinkedList<Object>();
        for (Invocation iv : this.calls) {
            result.add(iv.invoke());
        }
        return result;
    }

    public List<Invocation> getInvocations() {
        return Collections.unmodifiableList(this.calls);
    }

    public Iterator<Invocation> iterator() {
        return this.getInvocations().iterator();
    }
}
