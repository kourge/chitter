/**
 * A version of the FS interface that does not have conditional write operations
 * that are based on version numbers.
 */
public interface TransactionalFS {
    public boolean create(String filename);
    public boolean exists(String filename);
    public byte[] read(String filename);
    public boolean append(String filename, byte[] data);
    public boolean overwrite(String filename, byte[] data);
    public boolean delete(String filename);
}
