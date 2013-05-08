import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.Serializable;
import java.util.Arrays;
import edu.washington.cs.cse490h.lib.Callback;

/**
 * Encapsulates the invocation of a Java method, potentially divorced from
 * whatever class in which said method may be defined.
 *
 * A valid way to instantiate this class is to use the static method `of`,
 * which takes the class in which a method is defined and the name of the
 * method. Useful runtime information crucial for invocation via reflection
 * is automatically and naively extracted. Thus, an Invocation cannot be used
 * with overloaded methods.
 *
 * Another valid static constructor is `on`, which also takes an invocation
 * target. In this sense, the constructed Invocation is equivalent to a
 * Callback. In fact, `toCallback` can be called to perform the conversion.
 * The static constructor `call` is the constructor `on` chained with
 * `toCallback`. It provides a cleaner way of instantiating a Callback object.
 *
 * This class is fully serializable.
 */
public class Invocation implements Serializable {
    public static final long serialVersionUID = 0L;

    protected String procName;

    protected boolean hasReturnVal;
    protected Class<?> returnType;
    protected Object returnVal;

    protected Class<?>[] paramTypes;
    protected Object[] paramVals;

    protected int arity;

    protected transient Object target = null;

    protected Invocation(
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

    public static Invocation on(Object obj, String methodName) {
        Invocation iv = Invocation.of(obj.getClass(), methodName);
        iv.setTarget(obj);
        return iv;
    }

    public static Invocation on(Object obj, String methodName, Object... args) {
        Invocation iv = Invocation.on(obj, methodName);
        iv.setParameterValues(args);
        return iv;
    }

    public String getMethodName() {
        return this.procName;
    }

    public Method getMethod() {
        if (this.getTarget() == null) {
            return null;
        }

        Class<?> klass = this.getTarget().getClass();
        return Invocation.findMethod(klass, this.getMethodName());
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

    public void setParameterValues(Object... values) {
        if (values.length != this.arity) {
            throw new IllegalArgumentException("Incorrect param values arity");
        }

        this.paramVals = Arrays.copyOf(values, values.length);
    }

    public int getArity() {
        return this.arity;
    }

    public void setTarget(Object obj) {
        this.target = obj;
    }

    public Object getTarget() {
        return this.target;
    }

    public String toString() {
        return String.format(
            "(%s::%s%s -> %s%s)",
            this.target == null ? "" : this.target.getClass().getSimpleName(),
            this.procName, Arrays.toString(this.formatParams()),
            this.returnType.getSimpleName(),
            this.hasReturnVal ? "(" + this.returnVal + ")" : ""
        );
    }

    protected String[] formatParams() {
        String[] params = new String[this.paramTypes.length];

        for (int i = 0; i < this.paramTypes.length; i++) {
            Class<?> klass = this.paramTypes[i];
            params[i] = klass.getSimpleName();

            if (this.paramVals != null && this.paramVals.length != 0) {
                params[i] += "(" + this.paramVals[i].toString() + ")";
            }
        }

        return params;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Invocation)) {
            return false;
        }
        Invocation other = (Invocation)obj;

        return (
            this.equalsIgnoreValues(other) &&
            this.returnVal.equals(other.returnVal) &&
            Arrays.equals(this.paramVals, other.paramVals)
        );
    }

    public boolean equalsIgnoreValues(Invocation other) {
        return (
            this.procName.equals(other.procName) &&
            this.returnType.equals(other.returnType) &&
            Arrays.equals(this.paramTypes, other.paramTypes)
        );
    }

    public Object invokeOn(Object obj) throws InvocationException {
        this.setTarget(obj);
        return this.invoke();
    }

    public Object invoke() throws InvocationException {
        try {
            Method method = this.getMethod();
            Object result = method.invoke(this.getTarget(), this.getParameterValues());
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

    public Callback toCallback() {
        return new Callback(this.getMethod(), this.getTarget(), this.paramVals);
    }

    public static Callback call(Object obj, String methodName, Object... args) {
        return Invocation.on(obj, methodName, args).toCallback();
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

