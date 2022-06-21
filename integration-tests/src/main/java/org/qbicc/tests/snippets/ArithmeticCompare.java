package org.qbicc.tests.snippets;

// https://github.com/qbicc/qbicc/pull/219
import static org.qbicc.runtime.CNative.*;

public class ArithmeticCompare {
    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) {
        doubleLessThan();
        putchar(' ');

        doubleMoreThan();
        putchar(' ');

        floatLessThan();
        putchar(' ');

        floatMoreThan();
        putchar(' ');

        longLessThan();
        putchar(' ');

        longMoreThan();
        putchar(' ');

        javaLangByteCompare();
        putchar(' ');

        javaLangByteCompareUnsigned();
        putchar(' ');

        javaLangCharacterCompare();
        putchar(' ');

        javaLangIntegerCompare();
        putchar(' ');

        javaLangIntegerCompareUnsigned();
        putchar(' ');

        javaLangShortCompare();
        putchar(' ');

        javaLangShortCompareUnsigned();
    }

    static void doubleLessThan() {
        putchar(true == lessThanDoubleDouble(-0.01, 1.7765719460991445E308) ? '_' : 'F');
        putchar(false == lessThanDoubleDouble(0.14, -0.58) ? '_' : 'F');
    }

    // Do not remove. It helps avoid inlining of comparison result directly in bytecode.
    static boolean lessThanDoubleDouble(double v1, double v2) {
        return v1 < v2;
    }

    static void doubleMoreThan() {
        putchar(true == moreThanDoubleDouble(-0.08, -4.749661826852963E127) ? '_' : 'F');
        putchar(false == moreThanDoubleDouble(-7.339589655336246E74, -4.3319170163107416E41) ? '_' : 'F');
    }

    // Do not remove. It helps avoid inlining of comparison result directly in bytecode.
    static boolean moreThanDoubleDouble(double v1, double v2) {
        return v1 > v2;
    }

    static void floatLessThan() {
        putchar(true == lessThanFloatFloat(-0.91f, 1286789.0f) ? '_' : 'F');
        putchar(false == lessThanFloatFloat(0.06f, -1509.37f) ? '_' : 'F');
    }

    // Do not remove. It helps avoid inlining of comparison result directly in bytecode.
    static boolean lessThanFloatFloat(float v1, float v2) {
        return v1 < v2;
    }

    static void floatMoreThan() {
        putchar(false == moreThanFloatFloat(-9047.29f, 24.76f) ? '_' : 'F');
        putchar(true == moreThanFloatFloat(0.0f, -371.12f) ? '_' : 'F');
    }

    // Do not remove. It helps avoid inlining of comparison result directly in bytecode.
    static boolean moreThanFloatFloat(float v1, float v2) {
        return v1 > v2;
    }

    static void longLessThan() {
        putchar(true == lessThanLongLong(-40768L, 346211859L) ? '_' : 'F');
        putchar(false == lessThanLongLong(7941303730413368214L, -1487869016576975219L) ? '_' : 'F');
    }

    // Do not remove. It helps avoid inlining of comparison result directly in bytecode.
    static boolean lessThanLongLong(long v1, long v2) {
        return v1 < v2;
    }

    static void longMoreThan() {
        putchar(true == moreThanLongLong(5042L, 268L) ? '_' : 'F');
        putchar(false == moreThanLongLong(-9L, 496993L) ? '_' : 'F');
    }

    // Do not remove. It helps avoid inlining of comparison result directly in bytecode.
    static boolean moreThanLongLong(long v1, long v2) {
        return v1 > v2;
    }

    static void javaLangByteCompare() {
        putchar(Byte.compare((byte) 1, (byte) -2) > 0 ? '_' : 'F');
        putchar(Byte.compare((byte) 64, (byte) 64) == 0 ? '_' : 'F');
        putchar(Byte.compare((byte) -1, (byte) 2) < 0 ? '_' : 'F');
    }

    static void javaLangByteCompareUnsigned() {
        putchar(Byte.compareUnsigned((byte) -1, (byte) 2) > 0 ? '_' : 'F');
        putchar(Byte.compareUnsigned((byte) 64, (byte) 64) == 0 ? '_' : 'F');
        putchar(Byte.compareUnsigned((byte) 1, (byte) -2) < 0 ? '_' : 'F');
    }

    static void javaLangCharacterCompare() {
        putchar(Character.compare('b', 'a') > 0 ? '_' : 'F');
        putchar(Character.compare('z', 'z') == 0 ? '_' : 'F');
        putchar(Character.compare('a', 'b') < 0 ? '_' : 'F');
    }

    static void javaLangIntegerCompare() {
        putchar(Integer.compare(46095, -985998) > 0 ? '_' : 'F');
        putchar(Integer.compare(6095, -85998) >= 0 ? '_' : 'F');
        putchar(Integer.compare(2, 2) == 0 ? '_' : 'F');
        putchar(Integer.compare(2, -2) != 0 ? '_' : 'F');
        putchar(Integer.compare(-511, -8) < 0 ? '_' : 'F');
        putchar(Integer.compare(-51, -7) <= 0 ? '_' : 'F');

        putchar(0 > Integer.compare(-85998, 6095) ? '_' : 'F');
        putchar(0 >= Integer.compare(-5998, 95) ? '_' : 'F');
        putchar(0 == Integer.compare(3, 3) ? '_' : 'F');
        putchar(0 != Integer.compare(3, -3) ? '_' : 'F');
        putchar(0 < Integer.compare(-9, -11) ? '_' : 'F');
        putchar(0 <= Integer.compare(-5, -18) ? '_' : 'F');

        putchar(Integer.compare(32, -744865192) >= 1 ? '_' : 'F'); // IsLt(Cmp(a, b), 1)  => IsLe(a, b)
        putchar(Integer.compare(-5, 10) < 1 ? '_' : 'F');          // IsGe(Cmp(a, b), 1)  => IsGt(a, b)
        putchar(1 <= Integer.compare(497, -319) ? '_' : 'F');      // IsGt(1, Cmp(a, b))  => IsLe(a, b)
        putchar(1 > Integer.compare(-445866, -31029) ? '_' : 'F'); // IsLe(1, Cmp(a, b))  => IsGt(a, b)

        putchar(Integer.compare(-865192, 32) <= -1 ? '_' : 'F');   // IsGt(Cmp(a, b), -1) => IsGe(a, b)
        putchar(Integer.compare(8, -3) > -1 ? '_' : 'F');          // IsLe(Cmp(a, b), -1) => IsLt(a, b)
        putchar(-1 >= Integer.compare(-19, 97) ? '_' : 'F');       // IsLt(-1, Cmp(a, b)) => IsGe(a, b)
        putchar(-1 < Integer.compare(-1029, -45866) ? '_' : 'F');  // IsGe(-1, Cmp(a, b)) => IsLt(a, b)

        putchar(-Integer.compare(32, -744865192) < 0 ? '_' : 'F');  // Neg(Cmp(a, b)) -> Cmp(b, a)
    }

    static void javaLangIntegerCompareUnsigned() {
        putchar(Integer.compareUnsigned(-1, 2) > 0 ? '_' : 'F');
        putchar(Integer.compareUnsigned(-2, -2) == 0 ? '_' : 'F');
        putchar(Integer.compareUnsigned(222, -2389) < 0 ? '_' : 'F');
    }

    static void javaLangShortCompare() {
        putchar(Short.compare((short) 215, (short) -10) > 0 ? '_' : 'F');
        putchar(Short.compare((short) 32767, (short) 32767) == 0 ? '_' : 'F');
        putchar(Short.compare((short) 236, (short) 1891) < 0 ? '_' : 'F');
    }

    static void javaLangShortCompareUnsigned() {
        putchar(Short.compareUnsigned((short) -14, (short) 9564) > 0 ? '_' : 'F');
        putchar(Short.compareUnsigned((short) -32767, (short) -32767) == 0 ? '_' : 'F');
        putchar(Short.compareUnsigned((short) -25818, (short) -29) < 0 ? '_' : 'F');
    }
}