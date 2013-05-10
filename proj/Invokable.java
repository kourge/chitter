
public interface Invokable {
    public boolean equalsIgnoreValues(Invokable other);
    public Object invokeOn(Object obj) throws InvocationException;
    public Object invoke() throws InvocationException;
}
