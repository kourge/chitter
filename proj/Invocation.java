import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Encapsulates the invocation of a Java method, divorced from whatever class
 * in which said method may be defined.
 *
 * The only valid way to instantiate this class is to use the static method
 * `of`, which takes the class in which a method is defined and the name of the
 * method. Useful runtime information crucial for invocation via reflection
 * is automatically and naively extracted. Thus, an Invocation cannot be used
 * with overloaded methods.
 *
 * The actual invocation can be performed by calling `invokeOn`, which returns
 * the result of the invocation. This result can also be retrieved through
 * the `getReturnValue` method. Take caution and make sure that the method
 * signatures match when passing arguments. Nasal demons may occur if this
 * caution is not taken.
 *
 * This class is fully serializable.
 */
public class Invocation implements Serializable {
    public static final long serialVersionUID = 0L;

    private String procName;

    private boolean hasReturnVal;
    private Class<?> returnType;
    private Object returnVal;

    private Class<?>[] paramTypes;
    private Object[] paramVals;

    private int arity;

    private Invocation(
        String name, Class<?> returnType, Class<?>[] paramTypes, Object[] paramVals
    ) {
        this.procName = name;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
        this.paramVals = paramVals;
        this.arity = paramTypes.length;
    }

    public static Invocation of(Class<?> klass, String methodName) {
        Method method = Invocation.findMethod(klass, methodName);
        Class<?>[] parameterTypes = method.getParameterTypes();

        return new Invocation(
            methodName, method.getReturnType(), parameterTypes, null
        );
    }

    public static Invocation of(Class<?> klass, String methodName, Object... args) {
        Invocation iv = Invocation.of(klass, methodName);
        iv.setParameterValues(args);
        return iv;
    }

    public String getMethodName() {
        return this.procName;
    }

    public Class<?> getReturnType() {
        return this.returnType;
    }

    public Object getReturnValue() {
        return this.returnVal;
    }

    public void setReturnValue(Object value) {
        this.returnVal = value;
        this.hasReturnVal = true;
    }

    public void unsetReturnValue() {
        this.returnVal = null;
        this.hasReturnVal = false;
    }

    public Class<?>[] getParameterTypes() {
        return Arrays.copyOf(this.paramTypes, this.paramTypes.length);
    }

    public Object[] getParameterValues() {
        return Arrays.copyOf(this.paramVals, this.paramVals.length);
    }

    public void setParameterValues(Object[] values) {
        if (values.length != this.arity) {
            throw new IllegalArgumentException("Incorrect param values arity");
        }

        this.paramVals = values;
    }

    public String toString() {
        return String.format(
            "(::%s%s%s -> %s%s)",
            this.procName, Arrays.toString(this.paramTypes),
            this.paramVals == null ? "[void]" : Arrays.toString(this.paramVals),
            this.returnType.getName(),
            this.hasReturnVal ? "(" + this.returnVal + ")" : ""
        );
    }

    public Object invokeOn(Object obj) throws InvocationException {
        try {
            Class<?> klass = obj.getClass();
            Method method = Invocation.findMethod(klass, procName);
            Object result = method.invoke(obj, this.paramVals);
            this.setReturnValue(result);
            return result;
        } catch (IllegalAccessException e) {
            throw new InvocationException(e);
        } catch (IllegalArgumentException e) {
            throw new InvocationException(e);
        } catch (InvocationTargetException e) {
            throw new InvocationException(e);
        }
    }

    public static Method findMethod(Class<?> klass, String methodName) {
        for (Method method : klass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
}

