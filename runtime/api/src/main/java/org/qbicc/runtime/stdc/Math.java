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
    @extern
    @name("sinf")
    @NoThrow
    public static native float sin(float val);

    @extern
    @name("sin")
    @NoThrow
    public static native double sin(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("cosf")
    @NoThrow
    public static native float cos(float val);

    @extern
    @name("cos")
    @NoThrow
    public static native double cos(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("tanf")
    @NoThrow
    public static native float tan(float val);

    @extern
    @name("tan")
    @NoThrow
    public static native double tan(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("asinf")
    @NoThrow
    public static native float asin(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("asin")
    @NoThrow
    public static native double asin(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("acosf")
    @NoThrow
    public static native float acos(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("acos")
    @NoThrow
    public static native double acos(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("atanf")
    @NoThrow
    public static native float atan(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("atan")
    @NoThrow
    public static native double atan(double val);


    @extern
    @name("logf")
    @NoThrow
    public static native float log(float val);

    @extern
    @name("log")
    @NoThrow
    public static native double log(double val);


    @extern
    @name("log10f")
    @NoThrow
    public static native float log10(float val);

    @extern
    @name("log10")
    @NoThrow
    public static native double log10(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("sqrtf")
    @NoThrow
    public static native float sqrt(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("sqrt")
    @NoThrow
    public static native double sqrt(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("remainderf")
    @NoThrow
    public static native float remainder(float a, float b);

    @extern
    @name("remainder")
    @NoThrow
    public static native double remainder(double a, double b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("atan2f")
    @NoThrow
    public static native float atan2(float a, float b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("atan2")
    @NoThrow
    public static native double atan2(double a, double b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("sinhf")
    @NoThrow
    public static native float sinh(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("sinh")
    @NoThrow
    public static native double sinh(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("coshf")
    @NoThrow
    public static native float cosh(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("cosh")
    @NoThrow
    public static native double cosh(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("tanhf")
    @NoThrow
    public static native float tanh(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("tanh")
    @NoThrow
    public static native double tanh(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("expm1f")
    @NoThrow
    public static native float expm1(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("expm1")
    @NoThrow
    public static native double expm1(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("log1pf")
    @NoThrow
    public static native float log1p(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("log1p")
    @NoThrow
    public static native double log1p(double val);
}

