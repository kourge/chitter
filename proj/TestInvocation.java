import java.io.*;

public class TestInvocation {
    private String name;

    public TestInvocation(String name) {
        this.name = name;
    }

    public String say(int age) {
        return String.format(
            "Hello my name is %s and I am %d years old!", this.name, age
        );
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Invocation iv = Invocation.of(TestInvocation.class, "say", 56);
        System.out.println(iv);

        byte[] stream = Serialization.encode(iv);
        // transmission occurs
        Invocation iv2 = (Invocation)Serialization.decode(stream);

        try {
            TestInvocation t = new TestInvocation("Eustace");
            iv2.invokeOn(t);
        } catch (Exception e) {}

        byte[] stream2 = Serialization.encode(iv2);
        // transmission occurs
        Invocation iv3 = (Invocation)Serialization.decode(stream2);

        System.out.println(iv3);
        System.out.println(iv3.getReturnValue());
    }
}
