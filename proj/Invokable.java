
/**
 * Invokables are objects that represent one or more invocations and can be
 * invoked. Both Invocation (representing a single call) and Transaction
 * (representing multiple calls) implement Invokable.
 */
public interface Invokable {
    public boolean equalsIgnoreValues(Invokable other);
    public Object invokeOn(Object obj) throws InvocationException;
    public Object invoke() throws InvocationException;
    public Object getReturnValue();
    public void setReturnValue(Object result);
}
