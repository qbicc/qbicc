package org.qbicc.runtime.llvm;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdarg.*;
import static org.qbicc.runtime.stdc.Stdint.*;

import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;

@SuppressWarnings("RedundantSuppression")
public final class LLVM {

    // Variable argument lists

    @extern
    @name("llvm.va_start")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void va_start(ptr<va_list> arglist);

    @extern
    @name("llvm.va_end")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void va_end(ptr<va_list> arglist);

    @extern
    @name("llvm.va_copy")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void va_copy(ptr<va_list> dest_arglist, ptr<va_list> src_arglist);

    // Code generator

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.returnaddress")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<?> returnAddress(uint32_t level);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.addressofreturnaddress")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<?> addressOfReturnAddress();

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sponentry")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<?> spOnEntry();

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.frameaddress")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<?> frameAddress(uint32_t level);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.stacksave")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<?> stackSave();

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.stackrestore")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void stackRestore(ptr<?> ptr);

    @extern
    @name("llvm.prefetch")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void prefetch(ptr<?> address, uint32_t rw, uint32_t locality, uint32_t cacheType);

    @extern
    @name("llvm.thread.pointer")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<?> threadPointer();

    // Standard C

    @Inline
    @Hidden
    @SafePoint(SafePointBehavior.ALLOWED)
    public static int abs(int val) {
        return abs(val, false);
    }

    @Inline
    @Hidden
    @SafePoint(SafePointBehavior.ALLOWED)
    public static long abs(long val) {
        return abs(val, false);
    }

    @extern
    @name("llvm.abs.i32")
    @SafePoint(SafePointBehavior.ALLOWED)
    private static native int abs(int src, boolean is_int_min_poison);

    @extern
    @name("llvm.abs.i64")
    @SafePoint(SafePointBehavior.ALLOWED)
    private static native long abs(long src, boolean is_int_min_poison);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fabs.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float abs(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fabs.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double abs(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smin.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte min(byte a, byte b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umin.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint8_t min(uint8_t a, uint8_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smin.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short min(short a, short b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umin.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char min(char a, char b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smin.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int min(int a, int b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umin.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint32_t min(uint32_t a, uint32_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smin.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long min(long a, long b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umin.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint64_t min(uint64_t a, uint64_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.minimum.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float min(float a, float b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.minnum.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float min_libm(float a, float b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.minimum.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double min(double a, double b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.minnum.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double min_libm(double a, double b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smax.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte max(byte a, byte b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umax.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint8_t max(uint8_t a, uint8_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smax.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short max(short a, short b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umax.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char max(char a, char b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smax.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int max(int a, int b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umax.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint32_t max(uint32_t a, uint32_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smax.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long max(long a, long b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umax.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint64_t max(uint64_t a, uint64_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.maximum.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float max(float a, float b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.maxnum.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float max_libm(float a, float b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.maximum.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double max(double a, double b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.maxnum.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double max_libm(double a, double b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sqrt.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float sqrt(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sqrt.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double sqrt(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.powi.f32.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float powi(float val, int power);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.powi.f32.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float powi(float val, long power);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.powi.f64.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double powi(double val, int power);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.powi.f64.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double powi(double val, long power);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sin.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float sin(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sin.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double sin(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.cos.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float cos(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.cos.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double cos(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.pow.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float pow(float val, float power);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.pow.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double pow(double val, double power);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.exp.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float exp(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.exp.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double exp(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.exp2.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float exp2(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.exp2.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double exp2(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.log.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float log(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.log.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double log(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.log10.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float log10(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.log10.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double log10(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.log2.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float log2(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.log2.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double log2(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fma.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float fma(float a, float b, float c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fma.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double fma(double a, double b, double c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fmuladd.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float fmuladd(float a, float b, float c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fmuladd.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double fmuladd(double a, double b, double c);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.copysign.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float copySign(float mag, float sign);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.copysign.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double copySign(double mag, double sign);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.floor.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float floor(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.floor.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double floor(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ceil.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float ceil(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ceil.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double ceil(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.trunc.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float trunc(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.trunc.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double trunc(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.rint.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float roundToInt(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.rint.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double roundToInt(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.nearbyint.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float nearbyInt(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.nearbyint.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double nearbyInt(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.round.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float round(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.round.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double round(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.roundeven.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float roundEven(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.roundeven.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double roundEven(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.lround.i32.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int lround(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.lround.i32.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int lround(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.lround.i64.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long lround64(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.lround.i64.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long lround64(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.llround.i64.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long llround(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.llround.i64.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long llround(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.lrint.i32.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int lrint(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.lrint.i32.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int lrint(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.lrint.i64.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long lrint64(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.lrint.i64.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long lrint64(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.llrint.i64.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long llrint(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.llrint.i64.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long llrint(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fptosi.sat.i32.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int convertToInt(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fptosi.sat.i32.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int convertToInt(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fptoui.sat.i32.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int convertToIntUnsigned(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fptoui.sat.i32.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int convertToIntUnsigned(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fptosi.sat.i64.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long convertToLong(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fptosi.sat.i64.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long convertToLong(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fptoui.sat.i64.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long convertToLongUnsigned(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fptoui.sat.i64.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long convertToLongUnsigned(double val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.canonicalize.f32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float canonicalize(float val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.canonicalize.f64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double canonicalize(double val);

    // Bit manipulation

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bitreverse.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte bitReverse(byte val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bitreverse.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short bitReverse(short val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bitreverse.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char bitReverse(char val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bitreverse.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int bitReverse(int val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bitreverse.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long bitReverse(long val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bswap.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short byteSwap(short val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bswap.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char byteSwap(char val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bswap.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int byteSwap(int val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.bswap.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long byteSwap(long val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctpop.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte bitCount(byte val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctpop.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short bitCount(short val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctpop.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char bitCount(char val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctpop.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int bitCount(int val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctpop.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long bitCount(long val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctlz.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte numberOfLeadingZeros(byte val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctlz.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short numberOfLeadingZeros(short val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctlz.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char numberOfLeadingZeros(char val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctlz.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int numberOfLeadingZeros(int val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ctlz.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long numberOfLeadingZeros(long val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.cttz.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte numberOfTrailingZeros(byte val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.cttz.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short numberOfTrailingZeros(short val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.cttz.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char numberOfTrailingZeros(char val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.cttz.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int numberOfTrailingZeros(int val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.cttz.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long numberOfTrailingZeros(long val);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshl.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte funnelShiftLeft(byte a, byte b, byte c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshl.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short funnelShiftLeft(short a, short b, short c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshl.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char funnelShiftLeft(char a, char b, char c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshl.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int funnelShiftLeft(int a, int b, int c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshl.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long funnelShiftLeft(long a, long b, long c);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshr.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte funnelShiftRight(byte a, byte b, byte c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshr.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short funnelShiftRight(short a, short b, short c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshr.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char funnelShiftRight(char a, char b, char c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshr.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int funnelShiftRight(int a, int b, int c);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.fshr.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long funnelShiftRight(long a, long b, long c);

    // arithmetic with overflow

    @internal
    //@packed do not add padding between val and overflow
    public static final class byte_with_overflow {
        public byte val;
        public boolean overflow;
    }

    @internal
    //@packed do not add padding between val and overflow
    public static final class uint8_t_with_overflow {
        public uint8_t val;
        public boolean overflow;
    }

    @internal
    //@packed do not add padding between val and overflow
    public static final class short_with_overflow {
        public short val;
        public boolean overflow;
    }

    @internal
    //@packed do not add padding between val and overflow
    public static final class char_with_overflow {
        public char val;
        public boolean overflow;
    }

    @internal
    //@packed do not add padding between val and overflow
    public static final class int_with_overflow {
        public int val;
        public boolean overflow;
    }

    @internal
    //@packed do not add padding between val and overflow
    public static final class uint32_t_with_overflow {
        public uint32_t val;
        public boolean overflow;
    }

    @internal
    //@packed do not add padding between val and overflow
    public static final class long_with_overflow {
        public long val;
        public boolean overflow;
    }

    @internal
    //@packed do not add padding between val and overflow
    public static final class uint64_t_with_overflow {
        public uint64_t val;
        public boolean overflow;
    }


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sadd.with.overflow.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte_with_overflow addWithOverflow(byte a, byte b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.uadd.with.overflow.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint8_t_with_overflow addWithOverflow(uint8_t a, uint8_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sadd.with.overflow.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short_with_overflow addWithOverflow(short a, short b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.uadd.with.overflow.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char_with_overflow addWithOverflow(char a, char b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sadd.with.overflow.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int_with_overflow addWithOverflow(int a, int b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.uadd.with.overflow.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint32_t_with_overflow addWithOverflow(uint32_t a, uint32_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sadd.with.overflow.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long_with_overflow addWithOverflow(long a, long b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.uadd.with.overflow.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint64_t_with_overflow addWithOverflow(uint64_t a, uint64_t b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ssub.with.overflow.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte_with_overflow subWithOverflow(byte a, byte b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.usub.with.overflow.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint8_t_with_overflow subWithOverflow(uint8_t a, uint8_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ssub.with.overflow.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short_with_overflow subWithOverflow(short a, short b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.usub.with.overflow.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char_with_overflow subWithOverflow(char a, char b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ssub.with.overflow.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int_with_overflow subWithOverflow(int a, int b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.usub.with.overflow.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint32_t_with_overflow subWithOverflow(uint32_t a, uint32_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ssub.with.overflow.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long_with_overflow subWithOverflow(long a, long b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.usub.with.overflow.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint64_t_with_overflow subWithOverflow(uint64_t a, uint64_t b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smul.with.overflow.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte_with_overflow mulWithOverflow(byte a, byte b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umul.with.overflow.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint8_t_with_overflow mulWithOverflow(uint8_t a, uint8_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smul.with.overflow.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short_with_overflow mulWithOverflow(short a, short b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umul.with.overflow.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char_with_overflow mulWithOverflow(char a, char b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smul.with.overflow.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int_with_overflow mulWithOverflow(int a, int b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umul.with.overflow.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint32_t_with_overflow mulWithOverflow(uint32_t a, uint32_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.smul.with.overflow.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long_with_overflow mulWithOverflow(long a, long b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.umul.with.overflow.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint64_t_with_overflow mulWithOverflow(uint64_t a, uint64_t b);

    // saturation

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sadd.sat.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte addSaturating(byte a, byte b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.uadd.sat.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint8_t addSaturating(uint8_t a, uint8_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sadd.sat.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short addSaturating(short a, short b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.uadd.sat.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char addSaturating(char a, char b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sadd.sat.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int addSaturating(int a, int b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.uadd.sat.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint32_t addSaturating(uint32_t a, uint32_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sadd.sat.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long addSaturating(long a, long b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.uadd.sat.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint64_t addSaturating(uint64_t a, uint64_t b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ssub.sat.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte subSaturating(byte a, byte b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.usub.sat.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint8_t subSaturating(uint8_t a, uint8_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ssub.sat.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short subSaturating(short a, short b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.usub.sat.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char subSaturating(char a, char b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ssub.sat.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int subSaturating(int a, int b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.usub.sat.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint32_t subSaturating(uint32_t a, uint32_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ssub.sat.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long subSaturating(long a, long b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.usub.sat.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint64_t subSaturating(uint64_t a, uint64_t b);


    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sshl.sat.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte shiftLeftSaturating(byte a, byte b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ushl.sat.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint8_t shiftLeftSaturating(uint8_t a, uint8_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sshl.sat.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short shiftLeftSaturating(short a, short b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ushl.sat.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native char shiftLeftSaturating(char a, char b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sshl.sat.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int shiftLeftSaturating(int a, int b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ushl.sat.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint32_t shiftLeftSaturating(uint32_t a, uint32_t b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.sshl.sat.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long shiftLeftSaturating(long a, long b);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ushl.sat.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native uint64_t shiftLeftSaturating(uint64_t a, uint64_t b);

    // rounding mode

    public static final int ROUND_TOWARDS_ZERO = 0;
    public static final int ROUND_NEAREST_TIES_TO_EVEN = 1;
    public static final int ROUND_TOWARDS_POSITIVE_INFINITY = 2;
    public static final int ROUND_TOWARDS_NEGATIVE_INFINITY = 3;
    public static final int ROUND_NEAREST_TIES_AWAY_FROM_ZERO = 4;

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.flt.rounds")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int getRoundingMode();

    @extern
    @name("llvm.set.rounding")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void setRoundingMode(int mode);

    // traps

    @extern
    @name("llvm.trap")
    @NoReturn
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void trap();

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.debugtrap")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void debugTrap();

    // expect/assume

    @extern
    @name("llvm.expect.i1")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native boolean expect(boolean value, boolean expectedValue);

    @extern
    @name("llvm.expect.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte expect(byte value, byte expectedValue);

    @extern
    @name("llvm.expect.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short expect(short value, short expectedValue);

    @extern
    @name("llvm.expect.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int expect(int value, int expectedValue);

    @extern
    @name("llvm.expect.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long expect(long value, long expectedValue);

    @extern
    @name("llvm.expect.with.probability.i1")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native boolean expect(boolean value, boolean expectedValue, double probability);

    @extern
    @name("llvm.expect.with.probability.i8")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native byte expect(byte value, byte expectedValue, double probability);

    @extern
    @name("llvm.expect.with.probability.i16")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native short expect(short value, short expectedValue, double probability);

    @extern
    @name("llvm.expect.with.probability.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native int expect(int value, int expectedValue, double probability);

    @extern
    @name("llvm.expect.with.probability.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native long expect(long value, long expectedValue, double probability);

    @extern
    @name("llvm.assume")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void assume(boolean value);

    // fences/guards/misc

    @extern
    @name("llvm.arithmetic.fence")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native float arithmeticFence(float val);

    @extern
    @name("llvm.arithmetic.fence")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native double arithmeticFence(double val);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.donothing")
    @SafePoint(SafePointBehavior.ALLOWED)
    // can be called with invoke
    public static native void doNothing();

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.ptrmask")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native ptr<?> ptrMask(ptr<?> orig, long mask);

    // memory move/set

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memcpy.p0i8.p0i8.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memCopy(ptr<?> dest, ptr<@c_const ?> src, int size, boolean setToFalse);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memcpy.p0i8.p0i8.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memCopy(ptr<?> dest, ptr<@c_const ?> src, long size, boolean setToFalse);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memcpy.inline.p0i8.p0i8.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memCopyInline(ptr<?> dest, ptr<@c_const ?> src, int size, boolean setToFalse);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memcpy.inline.p0i8.p0i8.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memCopyInline(ptr<?> dest, ptr<@c_const ?> src, long size, boolean setToFalse);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memmove.p0i8.p0i8.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memMove(ptr<?> dest, ptr<@c_const ?> src, int size, boolean setToFalse);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memmove.p0i8.p0i8.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memMove(ptr<?> dest, ptr<@c_const ?> src, long size, boolean setToFalse);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memcpy.element.unordered.atomic.p0i8.p0i8.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memCopyUnordered(ptr<?> dest, ptr<@c_const ?> src, int size, int elementSize);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memcpy.element.unordered.atomic.p0i8.p0i8.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memCopyUnordered(ptr<?> dest, ptr<@c_const ?> src, int size, long elementSize);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memmove.element.unordered.atomic.p0i8.p0i8.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memMoveUnordered(ptr<?> dest, ptr<@c_const ?> src, int size, int elementSize);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memmove.element.unordered.atomic.p0i8.p0i8.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memMoveUnordered(ptr<?> dest, ptr<@c_const ?> src, int size, long elementSize);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memset.p0i8.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memSet(ptr<?> dest, byte val, int size, boolean setToFalse);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memset.p0i8.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memSet(ptr<?> dest, byte val, long size, boolean setToFalse);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memset.element.unordered.atomic.p0i8.i32")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memSetUnordered(ptr<?> dest, byte val, int size, int elementSize);

    @SuppressWarnings("SpellCheckingInspection")
    @extern
    @name("llvm.memset.element.unordered.atomic.p0i8.i64")
    @NoThrow
    @SafePoint(SafePointBehavior.ALLOWED)
    public static native void memSetUnordered(ptr<?> dest, byte val, long size, int elementSize);

    // inline assembly

    public static final int ASM_FLAG_SIDE_EFFECT = 1 << 0;
    public static final int ASM_FLAG_ALIGN_STACK = 1 << 1;
    public static final int ASM_FLAG_INTEL_DIALECT = 1 << 2;
    public static final int ASM_FLAG_UNWIND = 1 << 3;
    public static final int ASM_FLAG_IMPLICIT_SIDE_EFFECT = 1 << 4;
    public static final int ASM_FLAG_NO_RETURN = 1 << 5;

    public static native <T extends object> T asm(Class<T> returnType, String instruction, String operands, int flags, object... args);

}

