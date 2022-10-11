package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForStrictMath {
    HooksForStrictMath() {
    }

    @Hook
    static double sin(VmThread thread, double input) {
        return StrictMath.sin(input);
    }

    @Hook
    static double cos(VmThread thread, double input) {
        return StrictMath.cos(input);
    }

    @Hook
    static double tan(VmThread thread, double input) {
        return StrictMath.tan(input);
    }

    @Hook
    static double asin(VmThread thread, double input) {
        return StrictMath.asin(input);
    }

    @Hook
    static double acos(VmThread thread, double input) {
        return StrictMath.acos(input);
    }

    @Hook
    static double atan(VmThread thread, double input) {
        return StrictMath.atan(input);
    }

    @Hook
    static double log(VmThread thread, double input) {
        return StrictMath.log(input);
    }

    @Hook
    static double log10(VmThread thread, double input) {
        return StrictMath.log10(input);
    }

    @Hook
    static double sqrt(VmThread thread, double input) {
        return StrictMath.sqrt(input);
    }

    @Hook
    static double IEEEremainder(VmThread thread, double f1, double f2) {
        return StrictMath.IEEEremainder(f1, f2);
    }

    @Hook
    static double atan2(VmThread thread, double x, double y) {
        return StrictMath.atan2(x, y);
    }

    @Hook
    static double sinh(VmThread thread, double input) {
        return StrictMath.sinh(input);
    }

    @Hook
    static double cosh(VmThread thread, double input) {
        return StrictMath.cosh(input);
    }

    @Hook
    static double tanh(VmThread thread, double input) {
        return StrictMath.tanh(input);
    }

    @Hook
    static double expm1(VmThread thread, double input) {
        return StrictMath.expm1(input);
    }

    @Hook
    static double log1p(VmThread thread, double input) {
        return StrictMath.log1p(input);
    }
}
