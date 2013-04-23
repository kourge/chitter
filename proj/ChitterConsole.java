import java.io.IOException;

import java.util.Set;
import java.util.HashSet;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;

public class ChitterConsole {
    private static Set<String> commands;

    static {
        commands = new HashSet<String>(ChitterOperation.Operation.getNames());
        commands.add("help");
    }

    private ConsoleReader console;

    public static void main(String[] args) {
        ChitterConsole c = new ChitterConsole();
        c.run();
    }

    public ChitterConsole() {
        try {
            console = new ConsoleReader();
            console.addCompleter(new StringsCompleter(commands));
            console.setPrompt("prompt> ");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String line = null;
            while ((line = console.readLine()) != null) {
                handle(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            TerminalFactory.get().restore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle(String line) {
        System.out.println(line);
    }
}
