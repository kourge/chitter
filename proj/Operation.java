import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Arrays;
import java.lang.annotation.*;
import java.lang.reflect.*;

public class Operation {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface Dispatcher {
        Class<?> proc();
        String desc();
    }

    public static void performOn(ClientServerNode node, String command)
    throws Exception {
        Scanner scanner = new Scanner(command);

        int destination = scanner.nextInt();
        String commandName = scanner.next();
        List<String> args = new ArrayList<String>();
        while (scanner.hasNext()) {
            args.add(scanner.next());
        }

        Method parser = parserForCommand(commandName);
        Dispatcher dispatcher = parser.getAnnotation(Dispatcher.class);
        Object[] arguments = {};
        arguments = (Object[])parser.invoke(null, args);

        Object[] parameters = new Object[arguments.length + 3];
        parameters[0] = node;
        parameters[1] = null;
        parameters[2] = destination;
        System.arraycopy(arguments, 0, parameters, 3, arguments.length);

        int maxConstructorArity = 0;
        Constructor<?> ctor = null;
        for (Constructor<?> constructor : dispatcher.proc().getConstructors()) {
            if (constructor.getParameterTypes().length > maxConstructorArity) {
                ctor = constructor;
            }
        }

        assert ctor.getParameterTypes().length == parameters.length;
        ChitterProcedure instance = (ChitterProcedure)ctor.newInstance(parameters);
        instance.call();
    }

    private static Method parserForCommand(String command) {
        for (Method method : Command.class.getMethods()) {
            if (method.getName().equals(command) &&
                method.isAnnotationPresent(Dispatcher.class)) {
                return method;
            }
        }
        return null;
    }

    private static Map<String, String> operations;
    static {
        operations = new HashMap<String, String>();
        for (Method method : Operation.class.getMethods()) {
            if (method.isAnnotationPresent(Dispatcher.class)) {
                Dispatcher dispatcher = method.getAnnotation(Dispatcher.class);
                operations.put(method.getName(), dispatcher.desc());
            }
        }
    }
    public static Map<String, String> getOperations() {
        return operations;
    }

    @Dispatcher(proc=CreateUserProcedure.class, desc="createUser username")
    public static Object[] createUser(List<String> args) {
        assert args.size() == 1;
        return new Object[] { args.get(0) };
    }

    @Dispatcher(proc=ChitProcedure.class, desc="chit username text")
    public static Object[] chit(List<String> args) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 1; i < args.size(); i++) {
            buffer.append(args.get(i));
        }

        return new Object[] { args.get(0), buffer.toString() };
    }

    @Dispatcher(proc=AddFollowerProcedure.class, desc="addFollower username follower")
    public static Object[] addFollower(List<String> args) {
        assert args.size() == 2;
        return new Object[] { args.get(0), args.get(1) };
    }

    @Dispatcher(
        proc=RemoveFollowerProcedure.class, desc="removeFollower username follower"
    )
    public static Object[] removeFollower(List<String> args) {
        assert args.size() == 2;
        return new Object[] { args.get(0), args.get(1) };
    }

    @Dispatcher(proc=GetChitsProcedure.class, desc="getChits username")
    public static Object[] getChits(List<String> args) {
        assert args.size() == 1;
        return new Object[] { args.get(0) };
    }
}
