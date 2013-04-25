import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.*;

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
        List<String> args = new ArrayList<String>();
        while (scanner.hasNext()) {
            args.add(scanner.next());
        }

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

    @Dispatcher({ "create", "exists", "read", "currentVersion", "delete" })
    public static Invocation dispatchUnary(String command, List<String> args) {
        assert args.size() == 1;
        String filename = args.get(0);
        return Invocation.of(FSCommands.class, command, filename);
    }

    @Dispatcher("hasChanged")
    public static Invocation dispatchBinary(String command, List<String> args) {
        assert args.size() == 2;
        String filename = args.get(0);
        long version = Long.parseLong(args.get(1));
        return Invocation.of(FSCommands.class, command, filename, version);
    }

    @Dispatcher({ "appendIfNotChanged", "overwriteIfNotChanged" })
    public static Invocation dispatchRest(String command, List<String> args) {
        String filename = args.get(0);
        long version = Long.parseLong(args.get(1));

        StringBuffer payload = new StringBuffer();
        for (int i = 2; i < args.size(); i++) {
            payload.append(args.get(i));
        }

        byte[] data = payload.toString().getBytes();
        return Invocation.of(FSCommands.class, command, filename, data, version);
    }
}
