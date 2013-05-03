import java.io.Serializable;

/** A generic pair class */
public class Pair<T, U> implements Serializable {

    public static final long serialVersionUID = 0L;

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

    public String toString() {
        return String.format("Pair.of(%s, %s)", t, u);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Pair)) {
            return false;
        }
        Pair other = (Pair)obj;

        return t.equals(other.first()) && u.equals(other.second());
    }
}
