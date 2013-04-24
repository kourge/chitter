package edu.washington.cs.cse490h.lib;

import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;

public class Console {
    private Set<String> commands;
    private ConsoleReader console;

    public Console(Map<String, String> consoleOperationsDescription) {
        try {
            commands = new HashSet<String>(consoleOperationsDescription.keySet());
        } catch (NullPointerException e) {
            commands = new HashSet<String>();
        }
        commands.add("help");

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
