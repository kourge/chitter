import java.util.List;

public class ChitterOperations {
    private ChitterFSOperations fs;

    public ChitterOperations(ChitterFSOperations fsOps) {
        this.fs = fsOps;
    }

    public boolean createUser(String username, String password) {
        return false;
    }

    public boolean verify(String username, String password) {
        return false;
    }

    public boolean chit(String username, String text) {
        return false;
    }

    public int addFollower(String username, String follower) {
        return 0;
    }

    public int removeFollower(String username, String follower) {
        return 0;
    }

    public List<Chit> getChits(String username) {
        return null;
    }
}
