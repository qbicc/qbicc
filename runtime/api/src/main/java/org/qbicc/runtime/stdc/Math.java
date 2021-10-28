package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.NoThrow;

/**
 *
 */
@SuppressWarnings("RedundantSuppression")
@include("<math.h>")
@lib("m")
// todo: -fno-math-errno on probes
public class Math {
    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "sinf")
    @NoThrow
    public static native float sin(float val);

    @extern(withName = "sin")
    @NoThrow
    public static native double sin(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "cosf")
    @NoThrow
    public static native float cos(float val);

    @extern(withName = "cos")
    @NoThrow
    public static native double cos(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "tanf")
    @NoThrow
    public static native float tan(float val);

    @extern(withName = "tan")
    @NoThrow
    public static native double tan(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "asinf")
    @NoThrow
    public static native float asin(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "asin")
    @NoThrow
    public static native double asin(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "acosf")
    @NoThrow
    public static native float acos(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "acos")
    @NoThrow
    public static native double acos(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "atanf")
    @NoThrow
    public static native float atan(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "atan")
    @NoThrow
    public static native double atan(double val);


    @extern(withName = "logf")
    @NoThrow
    public static native float log(float val);

    @extern(withName = "log")
    @NoThrow
    public static native double log(double val);


    @extern(withName = "log10f")
    @NoThrow
    public static native float log10(float val);

    @extern(withName = "log10")
    @NoThrow
    public static native double log10(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "sqrtf")
    @NoThrow
    public static native float sqrt(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "sqrt")
    @NoThrow
    public static native double sqrt(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "remainderf")
    @NoThrow
    public static native float remainder(float a, float b);

    @extern(withName = "remainder")
    @NoThrow
    public static native double remainder(double a, double b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "atan2f")
    @NoThrow
    public static native float atan2(float a, float b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "atan2")
    @NoThrow
    public static native double atan2(double a, double b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "sinhf")
    @NoThrow
    public static native float sinh(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "sinh")
    @NoThrow
    public static native double sinh(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "coshf")
    @NoThrow
    public static native float cosh(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "cosh")
    @NoThrow
    public static native double cosh(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "tanhf")
    @NoThrow
    public static native float tanh(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "tanh")
    @NoThrow
    public static native double tanh(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "expm1f")
    @NoThrow
    public static native float expm1(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "expm1")
    @NoThrow
    public static native double expm1(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "log1pf")
    @NoThrow
    public static native float log1p(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern(withName = "log1p")
    @NoThrow
    public static native double log1p(double val);
}

