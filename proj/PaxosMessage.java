public class PaxosMessage {
    public enum Type {
        PREPARE,
        PROMISE,
        ACCEPT,
        ACCEPTED
    }

    public Type type;
}
