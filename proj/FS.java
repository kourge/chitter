public interface FS {
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
}
