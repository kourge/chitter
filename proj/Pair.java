import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A generic, serializable Pair class, usually used when two return values are
 * needed. By implementing Iterable, the values of this class can be unpacked by
 * Python code running under Jython.
 */
public class Pair<T, U> implements Serializable, Iterable<Object> {
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

    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private int i;

            public boolean hasNext() {
                return i < 2;
            }

            public Object next() {
                switch (i++) {
                case 0: return (Object)first();
                case 1: return (Object)second();
                default: throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
