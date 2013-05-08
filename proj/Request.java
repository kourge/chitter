import java.io.Serializable;
import java.util.Random;
import edu.washington.cs.cse490h.lib.Utility;

public class Request implements Serializable {
    public static final long serialVersionUID = 0L;

    private int dest;
    private long seq;
    private Invocation iv;
    private transient Invocation onComplete;

    private Request(int dest, Invocation invocation, Invocation onComplete) {
        this.iv = invocation;
        this.seq = Utility.getRNG().nextLong();
        this.dest = dest;
        this.onComplete = onComplete;
    }

    public static Request to(int dest, Invocation invocation, Invocation onComplete) {
        return new Request(dest, invocation, onComplete);
    }

    public Invocation getInvocation() {
        return this.iv;
    }

    public Invocation getOnComplete() {
        return this.onComplete;
    }

    public long getSeq() {
        return this.seq;
    }

    public int getDestination() {
        return this.dest;
    }

    public void complete() throws InvocationException {
        if (onComplete != null) {
            if (onComplete.getArity() == 1) {
                onComplete.setParameterValues(this.iv.getReturnValue());
            } else if (onComplete.getArity() == 2) {
                onComplete.setParameterValues(this.iv.getReturnValue(), this);
            } else {
                throw new InvocationException("Arity of onComplete is not 1 or 2");
            }
            onComplete.invoke();
        }
    }

    @Override
    public String toString() {
        return String.format(
            "<Req dest=%d seq=%s %s>",
            this.dest, Long.toString(this.seq).substring(0, 7), this.iv
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Request)) {
            return false;
        }
        Request other = (Request)obj;

        return this.seq == other.seq && this.iv.equalsIgnoreValues(other.iv);
    }

    @Override
    public int hashCode() {
        return new Long(this.seq).hashCode();
    }
}
