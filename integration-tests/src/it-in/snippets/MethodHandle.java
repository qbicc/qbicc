import java.util.function.IntFunction;

public class MethodHandle {
    public static void main(String[] args) {
        for (int i = 0; i < 5; i ++) {
            System.out.print(apply(Integer::toString, i));
            System.out.print('.');
        }
    }

    private static String apply(IntFunction<String> fn, int value) {
        return fn.apply(value);
    }
}
