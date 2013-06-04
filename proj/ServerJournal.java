import java.io.*;
import java.util.*;
import edu.washington.cs.cse490h.lib.*;

/** Journal used to log and complete server operations, this will only store completed
 *  transaction IDs, basically we use this to recover a Set of transactions the server
 *  has completed (if a client resends a transaction ID we already finished, then we
 *  can just tell them we're already done). This is a bit of a stretch as far as
 *  extending 'Journal' since it's behavior is fairly distinct from other subclasses,
 *  but it is still convenient to reuse some code here. */
public class ServerJournal extends Journal {
    public ServerJournal(String filename, Node n) throws JournalException {
        super(filename, n);
    }

    public void execute(Serializable obj) {
        // TODO
    }
}
