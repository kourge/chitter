from edu.washington.cs.cse490h.lib import ConsoleType

from java.io import IOException
from java.util import HashSet

from jline import TerminalFactory
from jline.console import ConsoleReader
from jline.console.completer import StringsCompleter

class Console(ConsoleType):
    def __init__(self, client_addr, server_addr, operations_map, is_simulator=False):
        self.client_addr = client_addr
        self.server_addr = server_addr
        self.is_simulator = is_simulator

        self.operations = dict(operations_map)
        self.operations["help"] = "help <optional command>"
        self.operations["login"] = "login username"
        self.operations["logout"] = "logout"
        self.operations["echo"] = "echo text"
        self.operations["exit"] = "exit"

        self.aux_commands = {"help", "login", "logout", "echo", "exit"}

        self.op_commands = set(operations_map.keySet())
        self.all_commands = set(self.op_commands)
        self.all_commands.update(self.aux_commands)

        all_commands_set = HashSet()
        for x in self.all_commands:
            all_commands_set.add(x)

        try:
            self.console = ConsoleReader()
            self.console.addCompleter(StringsCompleter(all_commands_set))
            self.console.setPrompt("prompt> ")
        except IOException as err:
            err.printStackTrace()


    def readLine(self):
        line = ""
        try:
            line = self.console.readLine()
        except IOException as err:
            err.printStackTrace()

        return self.handle(line)


    def stop(self):
        try:
            TerminalFactory.get().restore()
        except Exception as err:
            err.printStackTrace()


    def handle(self, line):
        cmd = line.split()
        if len(cmd) < 1:
            return ""

        if cmd[0] in self.op_commands:
            if self.is_simulator:
                return "%d %d %s" % (self.client_addr, self.server_addr, line)
            else:
                return "%d %s" % (self.server_addr, line) 
        if cmd[0] in self.aux_commands:
            if cmd[0] == "help":
                self.help(cmd)
            elif cmd[0] == "login" or cmd[0] == "logout":
                print "This command is currently not supported."
            else:
                return line

        return ""

    
    def help(self, cmd):
        if len(cmd) > 0:
            print ""
            
            if len(cmd) == 1:
                print "***Usage***"
                for help_item in self.operations.items():
                    print "%s: %s" % help_item
            else:
                desc = self.operations[cmd[1]]
                if desc is not None:
                    print "***Usage***"
                    print "%s: %s" % (cmd[1], desc)
                else:
                    print "%s not found" % cmd[1]

            print ""
