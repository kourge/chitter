/**
 * A version of the FS interface that does not have conditional write operations
 * that are based on version numbers.
 */
public interface TransactionalFS {
    public long create(String filename);
    public boolean exists(String filename);
    public byte[] read(String filename);
    public long currentVersion(String filename);
    public long append(String filename, byte[] data);
    public long overwrite(String filename, byte[] data);
    public boolean hasChanged(String filename);
    public boolean delete(String filename);
}
