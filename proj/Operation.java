import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Arrays;
import java.lang.annotation.*;
import java.lang.reflect.*;

public class Operation {
    public static void performOn(ClientServerNode node, String command)
    throws Exception {
        Scanner scanner = new Scanner(command);

        int destination = scanner.nextInt();
        String commandName = scanner.next();
        String commandString = scanner.nextLine();

        Method maker = makerForCommand(commandName);
        System.out.println(maker);
        if (maker == null) {
            throw new IllegalArgumentException(
                "Operation '" + commandName + "' does not exist"
            );
        }

        assert maker.getParameterTypes().length == 4;
        ChitterProcedure instance = (ChitterProcedure)maker.invoke(
            null, node, null, destination, commandString
        );
        instance.call();
    }

    private static Method makerForCommand(String commandName) {
        Class<?> klass = classForCommand(commandName);
        if (klass == null) {
            return null;
        }

        for (Method method : klass.getMethods()) {
            if (method.getName().equals("make") &&
                Modifier.isStatic(method.getModifiers())) {
                return method;
            }
        }
        return null;
    }

    private static Class<?> classForCommand(String commandName) {
        char[] command = commandName.toCharArray();
        command[0] = Character.toUpperCase(command[0]);
        commandName = new String(command);
        try {
            return Class.forName(commandName + "Procedure");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static void printResult(Object obj) {
        System.out.println(obj);
    }

    private static Class<ChitterProcedure.Proc> p = ChitterProcedure.Proc.class;
    private static Class<?>[] procedures = {
        AddFollowerProcedure.class, ChitProcedure.class,
        CreateUserProcedure.class, GetChitsProcedure.class,
        GetFollowingsProcedure.class, GetTimelineProcedure.class,
        RemoveFollowerProcedure.class
    };
    private static Map<String, String> operations;
    static {
        operations = new HashMap<String, String>();
        for (Class<?> klass : procedures) {
            for (Method method : klass.getMethods()) {
                if (method.isAnnotationPresent(p)) {
                    ChitterProcedure.Proc proc = method.getAnnotation(p);
                    operations.put(proc.name(), proc.desc());
                }
            }
        }
    }

    public static Map<String, String> getOperations() {
        return operations;
    }

    public static boolean supports(String operationName) {
        return operations.containsKey(operationName);
    }
}
