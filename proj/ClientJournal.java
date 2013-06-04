import java.io.*;
import java.util.*;
import edu.washington.cs.cse490h.lib.*;

/** Journal used to log and complete client operations */
public class ClientJournal extends Journal {
    
    public ClientJournal(String filename, Node n) throws JournalException {
        super(filename, n);
    }

    public void execute(Serializable obj) {
        // TODO fill this in when the new transaction semantics are figured
    }
}
