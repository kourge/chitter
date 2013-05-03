import java.util.List;

public interface Op {
    public enum FollowerChangeResult {
        SUCCESS, FAILURE, ALREADY_EXISTS, DOES_NOT_EXIST
    };

    public boolean createUser(String username);
    public boolean verify(String username);
    public boolean chit(String username, String text);
    public FollowerChangeResult addFollower(String username, String follower);
    public FollowerChangeResult removeFollower(String username, String follower);
    public List<Chit> getChits(String username);
    public List<Pair<String, Long>> getFollowings(String username);
    public List<Chit> getTimeline(String username);
}
