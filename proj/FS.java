/**
 * The set of file system operations we define in design doc 1 that hinges
 * around file versions so that clients don't stomp all over each other.
 */
public interface FS {
    public long FAILURE = -1L;

    public Pair<byte[], Long> EMPTY_RESULT = Pair.of(new byte[] {}, FAILURE);

    /** Create a file by name, return version number */
    public long create(String filename);

    /** Gets whether or not a file exists */
    public boolean exists(String filename);

    /** Reads a file by name */
    public Pair<byte[], Long> read(String filename);

    /** Gives the latest version number of a file */
    public long currentVersion(String filename);

    /** Append to a file if not changed since version we have */
    public long appendIfNotChanged(String filename, byte[] data, long version);

    /** Write a file if not changed since the version we have */
    public long overwriteIfNotChanged(String filename, byte[] data, long version);

    /** Gets whether or not the file has changed from the version we have */
    public boolean hasChanged(String filename, long version);

    /** Delete a file by name */
    public boolean delete(String filename);

    /** Same as hasChanged, but having changed is considered a failure */
    public boolean isSameVersion(String filename, long version);
}
