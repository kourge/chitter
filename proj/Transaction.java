import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Arrays;
import java.io.Serializable;

public class Transaction
implements Serializable, Invokable, Iterable<Invocation> {
    public static final long serialVersionUID = 0L;

    protected Invocation[] calls;
    protected Object result;

    public Transaction(Invocation... args) {
        this.calls = Arrays.copyOf(args, args.length);
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
            this.beforeInvocation(iv);
            result.add(iv.invokeOn(obj));
            this.afterInvocation(iv);
        }
        this.result = result;
        return result;
    }

    public Object invoke() throws InvocationException {
        List<Object> result = new LinkedList<Object>();
        for (Invocation iv : this.calls) {
            this.beforeInvocation(iv);
            result.add(iv.invoke());
            this.afterInvocation(iv);
        }
        this.result = result;
        return result;
    }

    protected void beforeInvocation(Invocation iv) throws InvocationException {}

    protected void afterInvocation(Invocation iv) throws InvocationException {}

    public Invocation[] getInvocations() {
        return Arrays.copyOf(this.calls, this.calls.length);
    }

    public Iterator<Invocation> iterator() {
        return Collections.unmodifiableList(
            Arrays.asList(this.getInvocations())
        ).iterator();
    }

    public Object getReturnValue() {
        return this.result;
    }

    public void setReturnValue(Object result) {
        this.result = result;
    }
}
