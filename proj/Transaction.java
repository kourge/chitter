import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Arrays;
import java.io.Serializable;
import edu.washington.cs.cse490h.lib.Utility;

/**
 * Encapsulates multiple Invocations into one Invokable. When invoked, all the
 * invocations wrapped in the transaction are invoked in order. This by itself
 * does not provide an atomic property; the subclass FSTransaction is
 * responsible for all the dirty work.
 *
 * The empty methods beforeInvocation and afterInvocation are defined. They are
 * meant to be overridden by a subclass to add sophisticated behavior such as
 * caching and failure detection.
 */
public class Transaction
implements Serializable, Invokable, Iterable<Invocation> {
    public static final long serialVersionUID = 0L;

    protected Invocation[] calls;
    protected long seq;
    protected Object result;
    protected boolean failed;

    public Transaction(Invocation... args) {
        this.calls = Arrays.copyOf(args, args.length);
        this.seq = Utility.getRNG().nextLong();
    }

    public boolean equalsIgnoreValues(Invokable obj) {
        if (!(obj instanceof Transaction)) {
            return false;
        }
        Transaction other = (Transaction)obj;

        if (other.calls.length != this.calls.length) {
            return false;
        }

        for (int i = 0; i < this.calls.length; ++i) {
            if (!this.calls[i].equalsIgnoreValues(other.calls[i])) {
                return false;
            }
        }

        return true;
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

    public boolean isFailure() {
        return this.failed;
    }
}
