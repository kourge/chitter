package edu.washington.cs.cse490h.lib;

import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;

public class Console {
    private Set<String> opCommands;
    private Set<String> auxCommands = new HashSet<String>() {{
        add("help");
        add("echo");
        add("exit");
    }};
    private Map<String, String> descMap;
    private Boolean isSimulator;
    private ConsoleReader console;

    public Console(Map<String, String> consoleOperationsDescription) {
        this(consoleOperationsDescription, false);
    }

    public Console(Map<String, String> consoleOperationsDescription, boolean isSimulator) {
        descMap = new HashMap<String, String>(consoleOperationsDescription);
        descMap.put("echo", "Echoes text");
        descMap.put("exit", "Exits the console");

        this.isSimulator = isSimulator;

        try {
            opCommands = new HashSet<String>(consoleOperationsDescription.keySet());
        } catch (NullPointerException e) {
            opCommands = new HashSet<String>();
        }

        try {
            console = new ConsoleReader();

            Set<String> allCommands = new HashSet<String>();
            allCommands.addAll(opCommands);
            allCommands.addAll(auxCommands);

            console.addCompleter(new StringsCompleter(allCommands));
            console.setPrompt("prompt> ");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public String readLine() {
        String line = "";
        try {
            line = console.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return handle(line);
    }

    public void stop() {
        try {
            TerminalFactory.get().restore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String handle(String line) {
		String[] cmd = line.split("\\s+");
		if(cmd.length < 1) {
			System.err.println("Command is too short: " + line);
			return "";
		}

        // all op commands are passed to node 0 if the Console is for a Simulator
        if (opCommands.contains(cmd[0])) {
            return isSimulator ? "0 " + line : line;
        }

        // handle auxiliary commands
        if (auxCommands.contains(cmd[0])) {
            if (cmd[0].equals("help")) {
                help(cmd);
                return "";
            } else {
                return line;
            }
        }

        // ignore invalid commands
        return "";
    }

    private void help(String[] cmd) {
        System.out.println("helping you");
    }
}
