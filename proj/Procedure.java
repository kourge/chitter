import edu.washington.cs.cse490h.lib.Node;

public abstract class Procedure {
    protected ClientServerNode node;
    protected Invocation onComplete;
    private boolean isComplete;
    private int destination;

    protected void populate(
        ClientServerNode node, Invocation onComplete, int destination
    ) {
        this.node = node;
        this.onComplete = onComplete;
        this.destination = destination;
    }

    public abstract void call() throws Exception;

    /**
     * Pipes the result of the first invocation to the second invocation.
     * The first invocation must have no target.
     * The second invocation must have no preset parameters, but must take
     * exactly one parameter of type Object.
     *
     * This is somewhat reminiscent to Haskell's bind.
     */
    protected void doThen(Invocation iv, Invocation onComplete) {
        node.sendRPC(Request.to(destination, iv, onComplete));
    }

    protected void returnValue(Object value) throws InvocationException {
        if (isComplete) {
            throw new InvocationException("Already returned once");
        } else {
            if (onComplete != null) {
                onComplete.setParameterValues(new Object[] { value });
                onComplete.invoke();
            }
            isComplete = true;
        }
    }
}
