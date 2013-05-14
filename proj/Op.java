import java.util.List;

/**
 * A list Chitter operations supported.
 */
public interface Op {
    public enum FollowerChangeResult {
        SUCCESS, FAILURE, ALREADY_EXISTS, DOES_NOT_EXIST
    };

    /**
     * @return true if the user was successfully created, false otherwise
     */
    public boolean createUser(String username);

    /**
     * @return true if the username is valid, false otherwise
     */
    public boolean verify(String username);

    /**
     * @return true if tweeting under the username succeeded, false otherwise
     */
    public boolean chit(String username, String text);

    /**
     * Add follower as a follower of username.
     * @return FollowerChangeResult.ALREADY_EXISTS if the follower is already
     * following username, FollowerChangeResult.FAILURE if the operation failed
     * in some other way, FollowerChangeResult.SUCCESS otherwise
     */
    public FollowerChangeResult addFollower(String username, String follower);

    /**
     * Remove follower from the list of followers of username.
     * @return FollowerChangeResult.DOES_NOT_EXIST if follower is not following
     * username already, FollowerChangeResult.FAILURE if the operation failed in
     * some other way, FollowerChangeResult.SUCCESS otherwise
     */
    public FollowerChangeResult removeFollower(String username, String follower);

    /**
     * @return a List of Chits, or null if an error occurred
     */
    public List<Chit> getChits(String username);

    /**
     * @return a list of Pairs consisting of (followed, timestamp) where
     * followed is the user that username is following and timestamp is the time
     * when username started following followed, or null if an error occurred
     */
    public List<Pair<String, Long>> getFollowings(String username);

    /**
     * @return a list of Chits representing the timeline of username, in reverse
     * chronological order, omitting previously viewed Chits, or null if an
     * error occurred
     */
    public List<Chit> getTimeline(String username);
}
