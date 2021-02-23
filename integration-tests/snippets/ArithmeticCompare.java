// https://github.com/quarkuscc/qcc/pull/219
import static cc.quarkus.qcc.runtime.CNative.*;

public class ArithmeticCompare {
    @extern
    public static native int putchar(int arg);

    @export
    public static int main() {
        javaLangByteCompare();
        javaLangByteCompareUnsigned();
        javaLangCharacterCompare();
        javaLangIntegerCompare();
        javaLangIntegerCompareUnsigned();
        javaLangShortCompare();
        javaLangShortCompareUnsigned();
        return 0;
    }

    static void javaLangByteCompare() {
        putchar(Byte.compare((byte) 1, (byte) -2) > 0 ? '.' : 'F');
        putchar(Byte.compare((byte) 64, (byte) 64) == 0 ? '.' : 'F');
        putchar(Byte.compare((byte) -1, (byte) 2) < 0 ? '.' : 'F');
        putchar('\n');
    }

    static void javaLangByteCompareUnsigned() {
        putchar(Byte.compareUnsigned((byte) -1, (byte) 2) > 0 ? '.' : 'F');
        putchar(Byte.compareUnsigned((byte) 64, (byte) 64) == 0 ? '.' : 'F');
        putchar(Byte.compareUnsigned((byte) 1, (byte) -2) < 0 ? '.' : 'F');
        putchar('\n');
    }

    static void javaLangCharacterCompare() {
        putchar(Character.compare('b', 'a') > 0 ? '.' : 'F');
        putchar(Character.compare('z', 'z') == 0 ? '.' : 'F');
        putchar(Character.compare('a', 'b') < 0 ? '.' : 'F');
        putchar('\n');
    }

    static void javaLangIntegerCompare() {
        putchar(Integer.compare(46095, -985998) > 0 ? '.' : 'F');
        putchar(Integer.compare(2, 2) == 0 ? '.' : 'F');
        putchar(Integer.compare(-511, -8) < 0 ? '.' : 'F');
        putchar('\n');
    }

    static void javaLangIntegerCompareUnsigned() {
        putchar(Integer.compareUnsigned(-1, 2) > 0 ? '.' : 'F');
        putchar(Integer.compareUnsigned(-2, -2) == 0 ? '.' : 'F');
        putchar(Integer.compareUnsigned(222, -2389) < 0 ? '.' : 'F');
        putchar('\n');
    }

    static void javaLangShortCompare() {
        putchar(Short.compare((short) 215, (short) -10) > 0 ? '.' : 'F');
        putchar(Short.compare((short) 32767, (short) 32767) == 0 ? '.' : 'F');
        putchar(Short.compare((short) 236, (short) 1891) < 0 ? '.' : 'F');
        putchar('\n');
    }

    static void javaLangShortCompareUnsigned() {
        putchar(Short.compareUnsigned((short) -14, (short) 9564) > 0 ? '.' : 'F');
        putchar(Short.compareUnsigned((short) -32767, (short) -32767) == 0 ? '.' : 'F');
        putchar(Short.compareUnsigned((short) -25818, (short) -29) < 0 ? '.' : 'F');
        putchar('\n');
    }

    public static void main(String[] args) {
        // make driver happy
    }
}