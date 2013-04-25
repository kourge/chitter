import java.io.Serializable;
import java.util.Random;

public class Request implements Serializable {
    public static final long serialVersionUID = 0L;

    private static final Random random;
    static {
        random = new Random();
    }

    private int dest;
    private long seq;
    private Invocation iv;
    private transient Invocation onComplete;

    private Request(int dest, Invocation invocation, Invocation onComplete) {
        this.iv = invocation;
        this.seq = random.nextLong();
        this.dest = dest;
        this.onComplete = onComplete;
    }

    public static Request to(int dest, Invocation invocation, Invocation onComplete) {
        return new Request(dest, invocation, onComplete);
    }

    public Invocation getInvocation() {
        return this.iv;
    }

    public long getSeq() {
        return this.seq;
    }

    public int getDestination() {
        return this.dest;
    }

    public void complete() throws InvocationException {
        if (onComplete != null) {
            onComplete.setParameterValues(new Object[] { this.iv.getReturnValue() });
            onComplete.invoke();
        }
    }

    @Override
    public String toString() {
        return String.format("<Req dest=%d seq=%d %s>", this.dest, this.seq, this.iv);
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
