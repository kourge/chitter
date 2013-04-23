import java.io.Serializable;

/** A generic pair class */
public class Pair<T, U> implements Serializable {

    private T t;
    private U u;

    public Pair(T t, U u) {
        this.t = t;
        this.u = u;
    }

    public static <M, N> Pair<M, N> of(M a, N b) {
        return new Pair<M, N>(a, b);
    }

    public T first() {
        return t;
    }

    public U second() {
        return u;
    }
}
