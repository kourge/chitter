package edu.washington.cs.cse490h.lib;

import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;

public class Console implements ConsoleType {
    private int clientAddr;
    private int serverAddr;
    private Set<String> opCommands;
    private Set<String> auxCommands = new HashSet<String>() {{
        add("help");
        add("login");
        add("logout");
        add("echo");
        add("exit");
    }};
    private Map<String, String> descMap;
    private Boolean isSimulator;
    private ConsoleReader console;
    private boolean loggedIn;

    public Console(int clientAddr, int serverAddr, Map<String, String> consoleOperationsDescription) {
        this(clientAddr, serverAddr, consoleOperationsDescription, false);
    }

    public Console(int clientAddr, int serverAddr, Map<String, String> consoleOperationsDescription, boolean isSimulator) {
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;

        if (consoleOperationsDescription != null) {
            descMap = new HashMap<String, String>(consoleOperationsDescription);
        } else {
            descMap = new HashMap<String, String>();
        }

        descMap.put("help", "help <optional command>");
        descMap.put("login", "login username");
        descMap.put("logout", "logout");
        descMap.put("echo", "echo text");
        descMap.put("exit", "exit");

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
        if (line == null) {
            return "exit";
        }
		String[] cmd = line.split("\\s+");
		if (cmd.length < 1) {
			System.err.println("Command is too short: " + line);
			return "";
		}

        // all op commands are passed to node 0 if the Console is for a Simulator
        if (opCommands.contains(cmd[0])) {
            return isSimulator ? String.format("%d %d %s", clientAddr, serverAddr, line)
                               : String.format("%d %s", serverAddr, line);
        }

        // handle auxiliary commands
        if (auxCommands.contains(cmd[0])) {
            if (cmd[0].equals("help")) {
                help(cmd);
                return "";
            } else if(cmd[0].equals("login") || cmd[0].equals("logout")) {
                System.out.println("This command is currently not supported.");
            } else {
                return line;
            }
        }

        // ignore invalid commands
        return "";
    }

    private void help(String[] cmd) {
        if (cmd.length > 0) {
            System.out.println();

            if (cmd.length == 1) {
                System.out.println("***Usage***");
                for (String helpItem : descMap.keySet()) {
                    System.out.println(helpItem + ": " + descMap.get(helpItem));
                }
            } else {
                String desc = descMap.get(cmd[1]);
                if (desc != null) {
                    System.out.println("***Usage***");
                    System.out.println(cmd[1] + ": " + desc);
                } else {
                    System.out.println(cmd[1] + " not found");
                }
            }

            System.out.println();
        }
    }
}
