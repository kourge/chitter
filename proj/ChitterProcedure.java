import java.lang.annotation.*;

public abstract class ChitterProcedure extends Procedure {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface Proc {
        String name();
        String desc();
    }

    protected static final Class<?> fs = FS.class;
    protected static final long FAILURE = -1;

    private static final char[] INVALID_USERNAME_CHARACTERS = { '\t', '\n' };

    protected static void checkUsername(String username) {
        for (char invalidCharacter : INVALID_USERNAME_CHARACTERS) {
            if (username.indexOf(invalidCharacter) != -1) {
                throw new IllegalArgumentException();
            }
        }
    }
}
