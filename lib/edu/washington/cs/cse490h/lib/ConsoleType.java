package edu.washington.cs.cse490h.lib;

public interface ConsoleType {
    public String readLine();
    public void stop(); 
    public String handle(String line);
    public void help(String[] cmd);
}
