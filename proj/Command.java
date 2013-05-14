import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.*;

/**
 * A utility class for converting a simple String file system command to a
 * Request object suitable to be passed to ClientServerNode::sendRPC. High-level
 * Twitter operations do not belong here; refer to the Operation class for
 * those. This still exists for mostly debugging purposes.
 * 
 * Performs a bunch of grunt work such as parsing and scanning through the magic
 * of reflection and annotations.
 */
public class Command {
    private Command() {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface Dispatcher {
        String[] value();
    }

    public static Request asRequest(String command) {
        Scanner scanner = new Scanner(command);

        int destination = scanner.nextInt();
        String commandName = scanner.next();
        String args = scanner.nextLine();

        Method method = methodForCommand(commandName);
        if (method == null) {
            return null;
        }

        try {
            Invocation iv = (Invocation)method.invoke(null, commandName, args);
            return Request.to(destination, iv, null);
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

    private static Method methodForCommand(String command) {
        for (Method method : Command.class.getMethods()) {
            if (method.isAnnotationPresent(Dispatcher.class)) {
                Dispatcher dispatcher = method.getAnnotation(Dispatcher.class);
                for (String supportedCommand : dispatcher.value()) {
                    if (supportedCommand.equals(command)) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    private static String[] commands;
    static {
        List<String> list = new ArrayList<String>();
        for (Method method : Command.class.getMethods()) {
            if (method.isAnnotationPresent(Dispatcher.class)) {
                Dispatcher dispatcher = method.getAnnotation(Dispatcher.class);
                for (String supportedCommand : dispatcher.value()) {
                    list.add(supportedCommand);
                }
            }
        }
        commands = list.toArray(new String[] {});
    }
    public static String[] getCommands() {
        return commands;
    }

    public static boolean supports(String commandName) {
        for (String supportedCommand : commands) {
            if (commandName.equals(supportedCommand)) {
                return true;
            }
        }
        return false;
    }

    @Dispatcher({ "create", "exists", "read", "currentVersion", "delete" })
    public static Invocation dispatchUnary(String command, String args) {
        String filename = (new Scanner(args)).next();
        return Invocation.of(FS.class, command, filename);
    }

    @Dispatcher("hasChanged")
    public static Invocation dispatchBinary(String command, String args) {
        Scanner scanner = new Scanner(args);
        String filename = scanner.next();
        long version = scanner.nextLong();
        return Invocation.of(FS.class, command, filename, version);
    }

    @Dispatcher({ "appendIfNotChanged", "overwriteIfNotChanged" })
    public static Invocation dispatchRest(String command, String args) {
        Scanner scanner = new Scanner(args);
        String filename = scanner.next();
        long version = scanner.nextLong();
        byte[] data = scanner.nextLine().trim().getBytes();
        return Invocation.of(FS.class, command, filename, data, version);
    }
}
