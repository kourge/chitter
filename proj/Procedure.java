import edu.washington.cs.cse490h.lib.Node;

public abstract class Procedure {
    protected ClientServerNode node;
    protected Invocation onComplete;
    private boolean isComplete;

    protected void populate(ClientServerNode node, Invocation onComplete) {
        this.node = node;
        this.onComplete = onComplete;
    }

    public abstract void call() throws Exception;

    /**
     * Pipes the result of the first invocation to the second invocation.
     * The first invocation must have no target.
     * The second invocation must have no preset parameters, but must take
     * exactly one parameter of type Object.
     */
    protected void doThen(Invocation iv, Invocation onComplete) {
        /*
        node.send(iv, onComplete);
        */
        /*
        Object result = iv.invoke();
        onComplete.setParameterValues(new Object[] { result });
        onComplete.invoke();
        */
    }

    protected void returnValue(Object value) throws InvocationException {
        if (isComplete) {
            ;
        } else {
            onComplete.setParameterValues(new Object[] { value });
            onComplete.invoke();
            isComplete = true;
        }
    }
}
