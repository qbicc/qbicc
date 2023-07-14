package org.qbicc.runtime;

import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.String.*;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Types, constants, variables, and methods that pertain to the <em>target</em> native environment from the perspective
 * of the C language. In general, types which are <em>all lowercase</em> indicate association with the C language
 * which uses that convention. When a C type or keyword conflicts syntactically with a Java type or keyword, the
 * prefix {@code c_} is prepended to the name to disambiguate them. When a C type contains a multiple keywords, the
 * corresponding {@code CNative} type will be delimited by underscores ({@code '_'}).
 * <p>
 * There is no mapping for the C {@code void} pseudo-type.  Methods which return {@code void} in C should return {@code void}
 * in Java.  Pointers to {@code void} should be represented using the wildcard {@code ?} symbol e.g. {@code ptr<?>}.
 */
public final class CNative {
    private CNative() {
    }

    /**
     * Determine if the given {@code thing} is a defined macro. The value of {@code thing} must be
     * a {@code static final} field whose value was initialized by {@link #constant()}. Macro definition tests are
     * run at build time and can be consumed from initialization stages. If the target is virtual (Java-in-Java),
     * no macros are defined.
     *
     * @param thing the thing to test
     * @return {@code true} if {@code thing} is a defined macro, {@code false} otherwise
     */
    public static native boolean defined(object thing);

    /**
     * Determine whether the given native value type is complete.
     *
     * @param val the native value type
     * @return {@code true} if the type is complete, {@code false} otherwise
     */
    public static native boolean isComplete(Class<? extends object> val);

    /**
     * Determine whether the given native value type is a signed integer.
     *
     * @param val the value type to examine
     * @return {@code true} if the value type is a signed integer, or {@code false} if it is not a signed integer
     */
    public static native boolean isSigned(Class<? extends word> val);

    /**
     * Determine whether the given native value type is an unsigned integer.
     *
     * @param val the value type to examine
     * @return {@code true} if the value type is an unsigned integer, or {@code false} if it is not an unsigned integer
     */
    public static native boolean isUnsigned(Class<? extends word> val);

    /**
     * Determine if two different native types are exactly equivalent.
     *
     * @param type1 the first type
     * @param type2 the second type
     * @return {@code true} if the two types are strictly equivalent, or {@code false} if they are not or it cannot be
     *         determined
     */
    public static native boolean typesAreEquivalent(Class<? extends object> type1, Class<? extends object> type2);

    public static native size_t sizeof(object[] array);

    public static native size_t sizeof(object obj);

    public static native size_t sizeofArray(Class<? extends object[]> obj);

    public static native size_t sizeof(Class<? extends object> obj);

    public static native size_t alignof(object obj);

    public static native size_t alignof(Class<? extends object> obj);

    public static <T extends object> int elementCount(T[] array) {
        return (int) (sizeof(array).longValue() / sizeof(array[0]).longValue());
    }

    /**
     * Get the offset of a member of a native object. The expression must statically resolve to a field read of a member of a
     * native object.  Note that this returns a {@link size_t} as per the specification for the corresponding C macro.
     *
     * @param expr the member
     * @return the offset of the member
     */
    public static native size_t offsetof(object expr);

    /**
     * A placeholder function to represent a C-defined macro or constant. The result of this method
     * must be directly assigned to a {@code static final} field of an appropriate object
     * type with a name corresponding to the constant to evaluate.
     * The value will be replaced with the result of the expansion of the given C constant. The location
     * of errors relating to evaluation of the constant will be set to the point where this method is called,
     * where it is appropriate to do so.
     * <p>
     * Constant values assigned to a field of type {@code object} may be used to test if the constant is a
     * macro which was {@link #defined(object)}.
     * <p>
     * Constant values may be used during initialization for some purposes.
     *
     * @param <T> the object type
     * @return the constant value
     */
    public static native <T extends object> T constant();

    /**
     * Get a pointer to the given object. If the object can be statically determined to have been allocated with
     * automatic storage duration, this may cause the object to retroactively have been allocated in memory (on the stack)
     * as opposed to a register or non-physical storage.
     * <p>
     * Passing in an array will yield a pointer to the array, which might not be what is desired. In such cases,
     * the first array element (i.e. the element whose index is zero) can be passed in to get a pointer to the start
     * of the array.
     * <p>
     * If the object is located within a Java object, the results may be unpredictable unless the object has been
     * pinned through a GC-specific mechanism. In general, getting a pointer to member of a Java object is discouraged.
     *
     * @param obj the object whose address is to be taken
     * @param <T> the object type
     * @return a pointer to the object
     */
    public static native <T, P extends ptr<T>> P addr_of(T obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array, or a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native ptr<int64_t> addr_of(long obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native ptr<int32_t> addr_of(int obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native ptr<int16_t> addr_of(short obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native ptr<int8_t> addr_of(byte obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native ptr<uint16_t> addr_of(char obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native ptr<_Bool> addr_of(boolean obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native ptr<_Float32> addr_of(float obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native ptr<_Float64> addr_of(double obj);

    /**
     * Get the pointer to a structure containing the given member.  The actual return type will be the type of
     * the structure base.
     *
     * @param memberPtr the pointer to the structure member
     * @param memberTemplate the member template, generally in "{@code null}" form e.g. {@code ((struct_type)null).member}
     * @param <M> the member type
     * @param <P> the resultant pointer type
     * @return the base pointer
     */
    public static native <M extends object, P extends ptr<?>> P base_of(ptr<M> memberPtr, M memberTemplate);

    /**
     * Get an object with automatic storage duration that is initialized to zero. The object type must not be
     * incomplete. The object may be stored on the stack or in one or more registers, or may not have any physical
     * storage.
     *
     * @param <T> the object type
     * @return the object
     */
    public static native <T extends object> T zero();

    /**
     * Get an object with automatic storage duration. The object type must not be incomplete. The object
     * may be stored on the stack, in one or more registers, inline with an enclosing object, or it may not have any
     * physical storage. Note that calling the default constructor of an {@link object} is equivalent to using
     * this method in most situations (that is, native objects are not normally zero-initialized).
     * <p>
     * Arrays <em>may</em> be constructed using this method, as long as the target array type is annotated
     * with {@link array_size}; however, in most cases it is syntactically simpler to use the normal Java array
     * constructor.
     *
     * @param <T> the object type
     * @return the object
     */
    public static native <T extends object> T auto();

    public static native <T extends object> T auto(T initial);
    public static native byte auto(byte initial);
    public static native char auto(char initial);
    public static native double auto(double initial);
    public static native float auto(float initial);
    public static native int auto(int initial);
    public static native long auto(long initial);
    public static native short auto(short initial);
    public static native boolean auto(boolean initial);

    /**
     * Make a word type instance directly out of the given value. If the word type is integral and signed,
     * the given value is sign-extended. If the word type is integral and unsigned, or is not integral, then
     * the given value is zero-extended and used exactly.
     *
     * @param intValue the integer value to use
     * @param <T> the word type
     * @return the word value
     */
    public static native <T extends word> T word(int intValue);

    /**
     * Make a word type instance directly out of the given value. If the word type is integral and signed,
     * the given value is sign-extended. If the word type is integral and unsigned, or is not integral, then
     * the given value is zero-extended and used exactly.
     *
     * @param longValue the integer value to use
     * @param <T> the word type
     * @return the word value
     */
    public static native <T extends word> T word(long longValue);

    /**
     * Make a word type instance directly out of the given value. The given value is zero-extended and used exactly.
     *
     * @param intValue the integer value to use
     * @param <T> the word type
     * @return the word value
     */
    public static native <T extends word> T uword(int intValue);

    /**
     * Make a word type instance directly out of the given value. The given value is zero-extended and used exactly.
     *
     * @param longValue the integer value to use
     * @param <T> the word type
     * @return the word value
     */
    public static native <T extends word> T uword(long longValue);

    /**
     * Make a word type instance directly out of the given value by zero-extending the raw 32-bit representation of the
     * given floating point value.
     *
     * @param floatValue the floating point value to use
     * @param <T> the word type
     * @return the word value
     */
    public static native <T extends word> T word(float floatValue);

    /**
     * Make a word type instance directly out of the given value by zero-extending the raw 64-bit representation of the
     * given floating point value.
     *
     * @param doubleValue the floating point value to use
     * @param <T> the word type
     * @return the word value
     */
    public static native <T extends word> T word(double doubleValue);

    public static native <T extends word> T word(boolean booleanValue);

    public static native int8_t wordExact(byte byteValue);

    public static native int16_t wordExact(short shortValue);

    public static native int32_t wordExact(int intValue);

    public static native int64_t wordExact(long longValue);

    public static native uint16_t wordExact(char charValue);

    public static native _Bool wordExact(boolean booleanValue);

    public static native _Float32 wordExact(float floatValue);

    public static native _Float64 wordExact(double doubleValue);

    /**
     * Get the bitwise-AND of two words, which must be the same type.
     *
     * @param w1 the first word
     * @param w2 the second word
     * @param <W> the word type
     * @return the result of the operation
     */
    public static native <W extends word> W wordAnd(W w1, W w2);

    /**
     * Get the bitwise-OR of two words, which must be the same type.
     *
     * @param w1 the first word
     * @param w2 the second word
     * @param <W> the word type
     * @return the result of the operation
     */
    public static native <W extends word> W wordOr(W w1, W w2);

    /**
     * Get the bitwise-exclusive-OR of two words, which must be the same type.
     *
     * @param w1 the first word
     * @param w2 the second word
     * @param <W> the word type
     * @return the result of the operation
     */
    public static native <W extends word> W wordXor(W w1, W w2);

    /**
     * Get the bitwise ones' complement (NOT) of a word.
     *
     * @param w the word
     * @param <W> the word type
     * @return the result of the operation
     */
    public static native <W extends word> W wordComp(W w);

    public static native <T, P extends ptr<T>> void fill(P ptr, T value, long cnt);

    public static native void clear(ptr<?> ptr);

    public static native void clear(ptr<?> ptr, long cnt);

    public static native <T> void copy(ptr<T> dest, ptr<T> src);

    public static native <T> void copy(ptr<T> dest, ptr<T> src, long cnt);

    public static native <T> void copy_rev(ptr<T> dest, ptr<T> src, long cnt);

    public static native <T> void swap(ptr<T> ptr1, ptr<T> ptr2);

    public static void copy(ptr<int8_t> dest, byte[] src, int srcOff, int len) {
        Objects.checkFromIndexSize(srcOff, len, src.length);
        copy(dest, addr_of(src[srcOff]), len);
    }

    public static void copy(ptr<int16_t> dest, short[] src, int srcOff, int len) {
        Objects.checkFromIndexSize(srcOff, len, src.length);
        copy(dest, addr_of(src[srcOff]), len);
    }

    public static void copy(ptr<int32_t> dest, int[] src, int srcOff, int len) {
        Objects.checkFromIndexSize(srcOff, len, src.length);
        copy(dest, addr_of(src[srcOff]), len);
    }

    public static void copy(ptr<int64_t> dest, long[] src, int srcOff, int len) {
        Objects.checkFromIndexSize(srcOff, len, src.length);
        copy(dest, addr_of(src[srcOff]), len);
    }

    public static void copy(ptr<uint16_t> dest, char[] src, int srcOff, int len) {
        Objects.checkFromIndexSize(srcOff, len, src.length);
        copy(dest, addr_of(src[srcOff]), len);
    }

    public static void copy(byte[] dest, int destOff, int destLen, ptr<@c_const int8_t> src) {
        Objects.checkFromIndexSize(destOff, destLen, dest.length);
        copy(addr_of(dest[destOff]).cast(), src.cast(), destLen);
    }

    public static void copy(short[] dest, int destOff, int destLen, ptr<@c_const int16_t> src) {
        Objects.checkFromIndexSize(destOff, destLen, dest.length);
        copy(addr_of(dest[destOff]).cast(), src.cast(), destLen);
    }

    public static void copy(int[] dest, int destOff, int destLen, ptr<@c_const int32_t> src) {
        Objects.checkFromIndexSize(destOff, destLen, dest.length);
        copy(addr_of(dest[destOff]).cast(), src.cast(), destLen);
    }

    public static void copy(long[] dest, int destOff, int destLen, ptr<@c_const int64_t> src) {
        Objects.checkFromIndexSize(destOff, destLen, dest.length);
        copy(addr_of(dest[destOff]).cast(), src.cast(), destLen);
    }

    public static void copy(char[] dest, int destOff, int destLen, ptr<@c_const uint16_t> src) {
        Objects.checkFromIndexSize(destOff, destLen, dest.length);
        copy(addr_of(dest[destOff]).cast(), src.cast(), destLen);
    }

    public static String utf8zToJavaString(ptr<@c_const uint8_t> ptr) {
        return utf8ToJavaString(ptr, strlen(ptr.cast()).intValue());
    }

    public static String utf8ToJavaString(ptr<@c_const uint8_t> ptr, int len) {
        final byte[] bytes = new byte[len];
        copy(bytes, 0, len, ptr.cast());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String asciizToJavaString(ptr<@c_const c_char> ptr) {
        return asciiToJavaString(ptr, strlen(ptr).intValue());
    }

    public static String asciiToJavaString(ptr<@c_const c_char> ptr, int len) {
        final byte[] bytes = new byte[len];
        copy(bytes, 0, len, ptr.cast());
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    // conversions

    /**
     * <b>Warning:</b> this is a potentially dangerous operation and should only be performed by internal components.
     * Directly convert a pointer to a reference.
     *
     * @param ptr the pointer (may be {@code null})
     * @return the reference (may be {@code null})
     */
    public static native <T> T ptrToRef(ptr<T> ptr);

    /**
     * <b>Warning:</b> this is a potentially dangerous operation and should only be performed by internal components.
     * Directly convert a reference to a pointer.
     *
     * @param ref the reference (may be {@code null})
     * @return the pointer (may be {@code null})
     */
    public static native <T, P extends ptr<T>> P refToPtr(T ref);

    /**
     * Dereference the given pointer.
     * The returned value, if used in an expression, becomes a "lazy-load" of the given pointer with unshared semantics.
     * The address may be taken of values returned by this method.
     *
     * @param ptr the pointer
     * @return the dereferenced value
     * @param <T> the value type
     * @param <P> the pointer type
     */
    public static native <T, P extends ptr<T>> T deref(P ptr);

    /**
     * Dereference the given pointer as the given type.
     * The returned value, if used in an expression, becomes a "lazy-load" of the given pointer with unshared semantics.
     * The address may be taken of values returned by this method.
     *
     * @param ptr the pointer
     * @return the dereferenced value
     * @param <T> the value type
     */
    public static native <T> T deref(ptr<?> ptr, Class<T> typeClass);

    /**
     * Cast the given object.
     * Used to cast between apparently-unrelated types without a {@code javac} warning.
     * If a cast is impossible, a qbicc warning will still generally be produced.
     *
     * @param obj the object to cast
     * @return the cast object
     * @param <T> the target type
     */
    public native static <T> T cast(Object obj);

    // intrinsic
    @NoSideEffects
    private static native int floatToInt1(float fv);

    /**
     * Method which implements conversions from {@code float} to {@code int} type.  This method implements
     * the clamping semantics required for correct behavior.
     *
     * @param fv the {@code float} value
     * @return the clamped {@code int} value
     */
    @NoSideEffects
    @AutoQueued
    public static int floatToInt(float fv) {
        return floatToInt1(fv);
    }

    // intrinsic
    @NoSideEffects
    private static native long floatToLong1(float fv);

    /**
     * Method which implements conversions from {@code float} to {@code long} type.  This method implements
     * the clamping semantics required for correct behavior.
     *
     * @param fv the {@code float} value
     * @return the clamped {@code long} value
     */
    @NoSideEffects
    @AutoQueued
    public static long floatToLong(float fv) {
        return floatToLong1(fv);
    }

    // intrinsic
    @NoSideEffects
    private static native int doubleToInt1(double dv);

    /**
     * Method which implements conversions from {@code double} to {@code int} type.  This method implements
     * the clamping semantics required for correct behavior.
     *
     * @param dv the {@code double} value
     * @return the clamped {@code int} value
     */
    @NoSideEffects
    @AutoQueued
    public static int doubleToInt(double dv) {
        return doubleToInt1(dv);
    }

    // intrinsic
    @NoSideEffects
    private static native long doubleToLong1(double dv);

    /**
     * Method which implements conversions from {@code double} to {@code long} type.  This method implements
     * the clamping semantics required for correct behavior.
     *
     * @param dv the {@code double} value
     * @return the clamped {@code long} value
     */
    @NoSideEffects
    @AutoQueued
    public static long doubleToLong(double dv) {
        return doubleToLong1(dv);
    }

    // built-in

    /**
     * Allocate some bytes on the stack and return a pointer. To create an automatic-duration object directly,
     * use {@link #zero} or {@link #auto()}, or use an {@link object} subclass array constructor.
     *
     * @param size the size to allocate
     * @return the object
     */
    public static native <P extends ptr<?>> P alloca(size_t size);

    /**
     * Get a constant character pointer to the UTF-8 representation of the given string literal.  If the string
     * is not already zero-terminated, a zero is added to the end of the string.
     *
     * @param stringLiteral the string literal (must not be {@code null}, must be a literal)
     * @return the pointer to the string
     */
    public static native ptr<@c_const c_char> utf8z(String stringLiteral);

    /**
     * A native object. Native objects are allocated on the stack or in the system heap, or are otherwise externally
     * managed.
     * <p>
     * Native object types may be <em>complete</em> or <em>incomplete</em>. An object of a complete type may have its
     * size checked via {@link #sizeof(object)}, or be allocated on the stack or system heap using {@link #auto()},
     * {@link #zero}, etc., and pointers to objects of complete type may be dereferenced.
     * An object of incomplete type may not do any of these things; however, it is still allowed to pass around pointers
     * to objects of incomplete type.
     * <p>
     * Arrays of native objects are allocated on the stack when the standard Java array constructor is used.
     */
    public abstract static class object extends InlineObject {
        protected object() {
        }

        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        public final int hashCode() {
            return cast(int32_t.class).intValue();
        }

        public final int sizeInBytes() {
            return sizeof(this).intValue();
        }

        public final int alignment() {
            return alignof(this).intValue();
        }

        /**
         * Perform an unsafe cast from this type to another. If the cast type is smaller than the source type, only the
         * lowest-order bits are preserved. If the cast type is larger than the source type, zero- or sign-extension may
         * take place, depending on the types.
         * <p>
         * The target cast type is normally inferred, but sometimes it is not possible to infer the target type.
         * In these cases, for simple types the type can be specified, for example: {@code var result = obj.cast(int32_t.class);},
         * and for complex (generic) types the cast value must be assigned to a temporary variable of the correct type.
         *
         * @param <T> the type of the object
         * @return the cast object
         */
        public native final <T extends object> T cast();

        /**
         * A convenience variant of {@link #cast()} which allows a type token to be used for simple casts.
         *
         * @param clazz the type class
         * @param <T> the type of the object
         * @return the cast object
         */
        public native final <T extends object> T cast(Class<T> clazz);

        public final boolean fitsIntoByte() {
            return sizeof(this).intValue() <= Byte.BYTES;
        }

        public final boolean fitsIntoShort() {
            return sizeof(this).intValue() <= Short.BYTES;
        }

        public final boolean fitsIntoInt() {
            return sizeof(this).intValue() <= Integer.BYTES;
        }

        public final boolean fitsIntoLong() {
            return sizeof(this).intValue() <= Long.BYTES;
        }

        public final String toString() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * An object that is within the word size of the target platform and generally exhibits numerical properties.
     */
    public abstract static class word extends object {
        public native final long longValue();

        public final double doubleValue() {
            return Double.longBitsToDouble(longValue());
        }

        public native final int intValue();

        public native final long uintValue();

        public final float floatValue() {
            return Float.intBitsToFloat(intValue());
        }

        public native final short shortValue();

        public native final int ushortValue();

        public native final byte byteValue();

        public native final int ubyteValue();

        public native final char charValue();

        public native final boolean isZero();

        // these are all helpful aliases for readability

        public final boolean booleanValue() {
            return !isZero();
        }

        public final boolean isNonZero() {
            return !isZero();
        }

        public final boolean isNonNull() {
            return !isZero();
        }

        public final boolean isNull() {
            return isZero();
        }

        public final boolean isTrue() {
            return !isZero();
        }

        public final boolean isFalse() {
            return isZero();
        }

        public final boolean isNegative() {
            assert isSigned(getClass());
            return longValue() < 0;
        }

        public final boolean isNotNegative() {
            return isUnsigned(getClass()) || longValue() >= 0;
        }

        public final boolean isPositive() {
            return isUnsigned(getClass()) ? isNonZero() : longValue() > 0;
        }

        public final boolean isNotPositive() {
            return isUnsigned(getClass()) ? isZero() : longValue() <= 0;
        }

        public native boolean isLt(word other);

        public native boolean isGt(word other);

        public native boolean isLe(word other);

        public native boolean isGe(word other);
    }

    /**
     * An object which is a structure.
     * Each field of the object will have distinct, non-overlapping location in memory.
     */
    public static abstract class struct extends object {
    }

    /**
     * An object which is a union.
     * Each field of the object will have the same location in memory.
     */
    public static abstract class union extends object {
    }

    /**
     * The special type which corresponds to a value whose type is a run time type ID.
     */
    public static final class type_id extends word {
    }

    /**
     * The special type which corresponds to the header field of an object.
     */
    public static final class header_type extends word {
    }

    /**
     * The special type which corresponds to a bitwise representation of an object reference.
     *
     * @param <B> the upper bounds
     */
    public static final class reference<B> extends word {
        /**
         * Get a {@code reference} for the given object reference.
         *
         * @param ref the object reference or {@code null}
         * @return the {@code reference} or {@code null}
         * @param <B> the upper bounds
         */
        public static native <B> reference<B> of(B ref);

        /**
         * Get a {@code reference} from a word (e.g. register) value.
         *
         * @param val the word value
         * @return the {@code reference} or {@code null}
         * @param <B> the upper bounds
         */
        public static native <B> reference<B> fromWord(word val);

        /**
         * Get an object reference for a {@code reference}.
         *
         * @return the object reference
         */
        public native B toObject();
    }

    // basic types

    @name("char")
    @size(1) // by definition
    public static final class c_char extends word {}
    public static final class _Bool extends word {}

    // basic signed types

    @name("signed char")
    @size(1) // by definition
    @signed(true) // by definition
    public static final class signed_char extends word {}

    @name("short")
    @signed(true) // by definition
    public static final class c_short extends word {}

    @name("int")
    @signed(true) // by definition
    public static final class c_int extends word {}

    @name("long")
    @signed(true) // by definition
    public static final class c_long extends word {}

    @name("long long")
    @signed(true) // by definition
    public static final class long_long extends word {}

    // basic unsigned types

    @name("unsigned char")
    @size(1) // by definition
    @signed(false) // by definition
    public static final class unsigned_char extends word {}

    @name("unsigned short")
    @signed(false) // by definition
    public static final class unsigned_short extends word {}

    @name("unsigned int")
    @signed(false) // by definition
    public static final class unsigned_int extends word {}

    @name("unsigned long")
    @signed(false) // by definition
    public static final class unsigned_long extends word {}

    @name("unsigned long long")
    @signed(false) // by definition
    public static final class unsigned_long_long extends word {}

    // basic FP types

    @name("float")
    public static final class c_float extends word {}

    @name("double")
    public static final class c_double extends word {}

    @name("long double")
    public static final class long_double extends word {}

    @incomplete
    public static final class c_void extends object {}

    /**
     * A pointer to a location in memory whose type is a C native type.
     *
     * @param <T> the invariant pointer type
     */
    public static abstract class ptr<T> extends word {
        /**
         * Get an array view of this pointer value. The array points to the same address as
         * this pointer, but has an incomplete type (no size). If the object being pointed to
         * has an incomplete type, using this method will result in a compilation error.
         * <p>
         * Writing to this array has the same effect as calling {@link #plus(int) plus(index)}{@code .}{@link #storeUnshared storeUnshared(val)} on the corresponding
         * pointer; reading from this array has the same effect as calling {@link #plus(int) plus(index)}{@code .}{@link #loadUnshared}.
         * <p>
         * Native arrays have no run time bounds checking.
         *
         * @return the array view
         */
        public native T[] asArray();

        /**
         * Treating this pointer as an array, get the array element at the given index. This is equivalent
         * to {@linkplain #loadUnshared()} dereferencing} the pointer at the given {@linkplain #plus(int) offset}.
         * In C, this is equivalent to pointer addition.
         *
         * @param arrayIdx the array index
         * @return the value
         */
        public T get(int arrayIdx) {
            return plus(arrayIdx).loadUnshared();
        }

        public void set(int arrayIdx, T newVal) {
            plus(arrayIdx).storeUnshared(newVal);
        }

        public native T loadUnshared();
        public native <V> V loadUnshared(Class<V> pointeeType);
        public native T loadPlain();
        public native <V> V loadPlain(Class<V> pointeeType);
        public native T loadOpaque();
        public native <V> V loadOpaque(Class<V> pointeeType);
        public native T loadSingleAcquire();
        public native <V> V loadSingleAcquire(Class<V> pointeeType);
        public native T loadAcquire();
        public native <V> V loadAcquire(Class<V> pointeeType);
        public native T loadVolatile();
        public native <V> V loadVolatile(Class<V> pointeeType);

        public native void storeUnshared(T value);
        public native <V> void storeUnshared(Class<V> pointeeType, V value);
        public native void storePlain(T value);
        public native <V> void storePlain(Class<V> pointeeType, V value);
        public native void storeOpaque(T value);
        public native <V> void storeOpaque(Class<V> pointeeType, V value);
        public native void storeSingleRelease(T value);
        public native <V> void storeSingleRelease(Class<V> pointeeType, V value);
        public native void storeRelease(T value);
        public native <V> void storeRelease(Class<V> pointeeType, V value);
        public native void storeVolatile(T value);
        public native <V> void storeVolatile(Class<V> pointeeType, V value);

        public native boolean compareAndSetOpaque(T expect, T update);
        public native <V> boolean compareAndSetOpaque(Class<V> pointeeType, V expect, V update);
        public native boolean compareAndSetAcquire(T expect, T update);
        public native <V> boolean compareAndSetAcquire(Class<V> pointeeType, V expect, V update);
        public native boolean compareAndSetRelease(T expect, T update);
        public native <V> boolean compareAndSetRelease(Class<V> pointeeType, V expect, V update);
        public native boolean compareAndSet(T expect, T update);
        public native <V> boolean compareAndSet(Class<V> pointeeType, V expect, V update);

        public native boolean weakCompareAndSetOpaque(T expect, T update);
        public native <V> boolean weakCompareAndSetOpaque(Class<V> pointeeType, V expect, V update);
        public native boolean weakCompareAndSetAcquire(T expect, T update);
        public native <V> boolean weakCompareAndSetAcquire(Class<V> pointeeType, V expect, V update);
        public native boolean weakCompareAndSetRelease(T expect, T update);
        public native <V> boolean weakCompareAndSetRelease(Class<V> pointeeType, V expect, V update);
        public native boolean weakCompareAndSet(T expect, T update);
        public native <V> boolean weakCompareAndSet(Class<V> pointeeType, V expect, V update);

        public native T compareAndSwapOpaque(T expect, T update);
        public native <V> V compareAndSwapOpaque(Class<V> pointeeType, V expect, V update);
        public native T compareAndSwapAcquire(T expect, T update);
        public native <V> V compareAndSwapAcquire(Class<V> pointeeType, V expect, V update);
        public native T compareAndSwapRelease(T expect, T update);
        public native <V> V compareAndSwapRelease(Class<V> pointeeType, V expect, V update);
        public native T compareAndSwap(T expect, T update);
        public native <V> V compareAndSwap(Class<V> pointeeType, V expect, V update);

        public native T weakCompareAndSwapOpaque(T expect, T update);
        public native <V> V weakCompareAndSwapOpaque(Class<V> pointeeType, V expect, V update);
        public native T weakCompareAndSwapAcquire(T expect, T update);
        public native <V> V weakCompareAndSwapAcquire(Class<V> pointeeType, V expect, V update);
        public native T weakCompareAndSwapRelease(T expect, T update);
        public native <V> V weakCompareAndSwapRelease(Class<V> pointeeType, V expect, V update);
        public native T weakCompareAndSwap(T expect, T update);
        public native <V> V weakCompareAndSwap(Class<V> pointeeType, V expect, V update);

        public native T getAndSetOpaque(T newVal);
        public native <V> V getAndSetOpaque(Class<V> pointeeType, V newVal);
        public native T getAndSetAcquire(T newVal);
        public native <V> V getAndSetAcquire(Class<V> pointeeType, V newVal);
        public native T getAndSetRelease(T newVal);
        public native <V> V getAndSetRelease(Class<V> pointeeType, V newVal);
        public native T getAndSet(T newVal);
        public native <V> V getAndSet(Class<V> pointeeType, V newVal);

        public native T getAndSetMinOpaque(T otherVal);
        public native <V> V getAndSetMinOpaque(Class<V> pointeeType, V otherVal);
        public native T getAndSetMinAcquire(T otherVal);
        public native <V> V getAndSetMinAcquire(Class<V> pointeeType, V otherVal);
        public native T getAndSetMinRelease(T otherVal);
        public native <V> V getAndSetMinRelease(Class<V> pointeeType, V otherVal);
        public native T getAndSetMin(T otherVal);
        public native <V> V getAndSetMin(Class<V> pointeeType, V otherVal);

        public native T getAndSetMaxOpaque(T otherVal);
        public native <V> V getAndSetMaxOpaque(Class<V> pointeeType, V otherVal);
        public native T getAndSetMaxAcquire(T otherVal);
        public native <V> V getAndSetMaxAcquire(Class<V> pointeeType, V otherVal);
        public native T getAndSetMaxRelease(T otherVal);
        public native <V> V getAndSetMaxRelease(Class<V> pointeeType, V otherVal);
        public native T getAndSetMax(T otherVal);
        public native <V> V getAndSetMax(Class<V> pointeeType, V otherVal);

        public native T getAndAddOpaque(T addend);
        public native <V> V getAndAddOpaque(Class<V> pointeeType, V addend);
        public native T getAndAddAcquire(T addend);
        public native <V> V getAndAddAcquire(Class<V> pointeeType, V addend);
        public native T getAndAddRelease(T addend);
        public native <V> V getAndAddRelease(Class<V> pointeeType, V addend);
        public native T getAndAdd(T addend);
        public native <V> V getAndAdd(Class<V> pointeeType, V addend);

        public native T getAndSubtractOpaque(T subtrahend);
        public native <V> V getAndSubtractOpaque(Class<V> pointeeType, V subtrahend);
        public native T getAndSubtractAcquire(T subtrahend);
        public native <V> V getAndSubtractAcquire(Class<V> pointeeType, V subtrahend);
        public native T getAndSubtractRelease(T subtrahend);
        public native <V> V getAndSubtractRelease(Class<V> pointeeType, V subtrahend);
        public native T getAndSubtract(T subtrahend);
        public native <V> V getAndSubtract(Class<V> pointeeType, V subtrahend);

        public native T getAndBitwiseAndOpaque(T bits);
        public native <V> V getAndBitwiseAndOpaque(Class<V> pointeeType, V bits);
        public native T getAndBitwiseAndAcquire(T bits);
        public native <V> V getAndBitwiseAndAcquire(Class<V> pointeeType, V bits);
        public native T getAndBitwiseAndRelease(T bits);
        public native <V> V getAndBitwiseAndRelease(Class<V> pointeeType, V bits);
        public native T getAndBitwiseAnd(T bits);
        public native <V> V getAndBitwiseAnd(Class<V> pointeeType, V bits);

        public native T getAndBitwiseOrOpaque(T bits);
        public native <V> V getAndBitwiseOrOpaque(Class<V> pointeeType, V bits);
        public native T getAndBitwiseOrAcquire(T bits);
        public native <V> V getAndBitwiseOrAcquire(Class<V> pointeeType, V bits);
        public native T getAndBitwiseOrRelease(T bits);
        public native <V> V getAndBitwiseOrRelease(Class<V> pointeeType, V bits);
        public native T getAndBitwiseOr(T bits);
        public native <V> V getAndBitwiseOr(Class<V> pointeeType, V bits);

        public native T getAndBitwiseXorOpaque(T bits);
        public native <V> V getAndBitwiseXorOpaque(Class<V> pointeeType, V bits);
        public native T getAndBitwiseXorAcquire(T bits);
        public native <V> V getAndBitwiseXorAcquire(Class<V> pointeeType, V bits);
        public native T getAndBitwiseXorRelease(T bits);
        public native <V> V getAndBitwiseXorRelease(Class<V> pointeeType, V bits);
        public native T getAndBitwiseXor(T bits);
        public native <V> V getAndBitwiseXor(Class<V> pointeeType, V bits);

        public native T getAndBitwiseNandOpaque(T bits);
        public native <V> V getAndBitwiseNandOpaque(Class<V> pointeeType, V bits);
        public native T getAndBitwiseNandAcquire(T bits);
        public native <V> V getAndBitwiseNandAcquire(Class<V> pointeeType, V bits);
        public native T getAndBitwiseNandRelease(T bits);
        public native <V> V getAndBitwiseNandRelease(Class<V> pointeeType, V bits);
        public native T getAndBitwiseNand(T bits);
        public native <V> V getAndBitwiseNand(Class<V> pointeeType, V bits);

        /**
         * Get a pointer which is offset from the base by the given number of elements.
         *
         * @param offset the element offset
         * @return the offset pointer
         */
        public native <P extends ptr<T>> P plus(int offset);

        /**
         * Get a pointer which is offset from the base by the given number of elements.
         *
         * @param offset the element offset
         * @return the offset pointer
         */
        public native <P extends ptr<T>> P plus(long offset);

        /**
         * Get a pointer which is offset from the base by the given number of elements.
         *
         * @param offset the element offset
         * @return the offset pointer
         */
        public native <P extends ptr<T>> P minus(int offset);

        /**
         * Get a pointer which is offset from the base by the given number of elements.
         *
         * @param offset the element offset
         * @return the offset pointer
         */
        public native <P extends ptr<T>> P minus(long offset);

        /**
         * Get the difference between this pointer and another pointer of the same type. This is the number
         * of elements spanned by the two pointers. Pointers of {@code void} type are considered to have a size of 1.
         *
         * @param other the other pointer
         * @return the difference between the two pointers
         */
        public native ptrdiff_t minus(ptr<T> other);

        /**
         * Select a subfield from this pointer, which must be passed back through {@link #addr_of} in order
         * to be used.  The returned subfield has {@code void} type and thus cannot be accessed directly.
         *
         * @return the selection view of the pointee
         */
        public T sel() {
            return deref(this);
        }

        /**
         * Select a subfield from this pointer, which must be passed back through {@link #addr_of} in order
         * to be used.  The returned subfield has {@code void} type and thus cannot be accessed directly.
         *
         * @param <V> the pointee type
         * @param pointeeType the pointee type class (must not be {@code null})
         * @return the selection view of the pointee
         */
        public <V> V sel(Class<V> pointeeType) {
            return deref(this, pointeeType);
        }

        /**
         * Determine if this pointer instance is a pointer to a pinned Java data structure.
         *
         * @return {@code true} if this is a pointer to a pinned Java data structure, {@code false} if it is not or
         *         it cannot be determined
         */
        public native boolean isPinned();
    }

    /**
     * A function object. Function objects are always considered incomplete. If the function has an invokable interface,
     * that interface can be used to dereference the function pointer. The type of the invokable must be statically
     * known in order for the compiler to generate the appropriate prologue.
     *
     * @param <F> the function invokable interface type
     */
    @incomplete
    public static final class function<F> extends object {
        function() {
        }

        /**
         * Get a reference which can be used to invoke the function being pointed to by this object.
         *
         * @return the invokable reference
         */
        public native F asInvokable();

        public static native <F> function<F> of(F invokable);
    }

    // floating point

    // todo: revisit this with a more sophisticated probe
    @name("float")
    public static final class _Float32 extends word {}

    // todo: revisit this with a more sophisticated probe
    @name("double")
    public static final class _Float64 extends word {}

    // directives

    @Repeatable(lib.List.class)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @Retention(RetentionPolicy.CLASS)
    public @interface lib {
        /**
         * The C library name to include. The underlying linker will include the library using the platform-specific
         * mechanism.
         *
         * @return the library name
         */
        String value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
        @Retention(RetentionPolicy.CLASS)
        @interface List {
            lib[] value();
        }
    }

    @Repeatable(include.List.class)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @Retention(RetentionPolicy.CLASS)
    public @interface include {
        /**
         * The C include name, including the quotes, which may either be {@code ""} or {@code <>} as appropriate.
         *
         * @return the include name
         */
        String value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
        @Retention(RetentionPolicy.CLASS)
        @interface List {
            include[] value();
        }
    }

    @Repeatable(define.List.class)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @Retention(RetentionPolicy.CLASS)
    public @interface define {
        /**
         * The name of the symbol being defined. Must not be empty.
         *
         * @return the name
         */
        String value();

        /**
         * The value that is being defined.
         *
         * @return the value
         */
        String as() default "";

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
        @Retention(RetentionPolicy.CLASS)
        @interface List {
            define[] value();
        }
    }

    @Repeatable(undef.List.class)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @Retention(RetentionPolicy.CLASS)
    public @interface undef {
        /**
         * The name of the symbol being undefined. Must not be empty.
         *
         * @return the name
         */
        String value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
        @Retention(RetentionPolicy.CLASS)
        @interface List {
            undef[] value();
        }
    }

    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.CLASS)
    public @interface macro {
    }

    @Repeatable(name.List.class)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface name {
        String value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
        @Retention(RetentionPolicy.CLASS)
        @Documented
        @interface List {
            name[] value();
        }
    }

    /**
     * Tag an object type declaration as "incomplete": size will not be probed, and therefore
     * any attempt to directly instantiate the type will be refused at compile time.
     * <p>
     * When used on a compound type member, that member is presumed to be inaccessible (e.g. a bit field).
     */
    @Target({ ElementType.TYPE, ElementType.FIELD })
    @Retention(RetentionPolicy.CLASS)
    @Repeatable(incomplete.List.class)
    @Documented
    public @interface incomplete {
        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target({ ElementType.TYPE, ElementType.FIELD })
        @Retention(RetentionPolicy.CLASS)
        @Documented
        @interface List {
            incomplete[] value();
        }
    }

    /**
     * Explicitly specify the exact size of an object.  The size will not be probed.  Different sizes can be specified
     * for different platforms.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @Repeatable(size.List.class)
    @Documented
    public @interface size {
        int value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.CLASS)
        @Documented
        @interface List {
            size[] value();
        }
    }

    /**
     * Explicitly specify the exact alignment of an object.  The alignment will not be probed.  Different alignments can be specified
     * for different platforms.  By convention, a value of Integer.MAX_VALUE encodes that the alignment should be max_align_t.
     */
    @Target({ ElementType.TYPE, ElementType.FIELD })
    @Retention(RetentionPolicy.CLASS)
    @Repeatable(align.List.class)
    @Documented
    public @interface align {
        int value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target({ ElementType.TYPE, ElementType.FIELD })
        @Retention(RetentionPolicy.CLASS)
        @Documented
        @interface List {
            align[] value();
        }
    }

    /**
     * Explicitly specify the exact alignment of an object as being equal to the alignment of the given type.
     * The alignment will not be probed.  Different alignments can be specified for different platforms.
     */
    @Target({ ElementType.TYPE, ElementType.FIELD })
    @Retention(RetentionPolicy.CLASS)
    @Repeatable(align_as.List.class)
    @Documented
    public @interface align_as {
        Class<?> value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target({ ElementType.TYPE, ElementType.FIELD })
        @Retention(RetentionPolicy.CLASS)
        @Documented
        @interface List {
            align_as[] value();
        }
    }

    /**
     * Explicitly specify the exact offset of an object member.  The offset will not be probed.  Different offsets can
     * be specified for different platforms.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.CLASS)
    @Repeatable(offset.List.class)
    @Documented
    public @interface offset {
        int value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.CLASS)
        @Documented
        @interface List {
            offset[] value();
        }
    }

    /**
     * Explicitly specify whether an object is signed.  The signedness of the object will not be probed.  Different
     * signedness can be specified for different platforms.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @Repeatable(signed.List.class)
    @Documented
    public @interface signed {
        boolean value();

        /**
         * Cause this annotation to take effect only if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] when() default {};

        /**
         * Prevent this annotation from taking effect if <em>all</em> of the given conditions return {@code true}.
         *
         * @return the condition classes
         */
        Class<? extends BooleanSupplier>[] unless() default {};

        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.CLASS)
        @Documented
        @interface List {
            signed[] value();
        }
    }

    /**
     * Import the annotated method or object as a linkable symbol. Native methods within a class annotated
     * with {@link include @include} are implicitly considered {@code extern}.
     */
    @Target({ ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface extern {
        String withName() default "";

        ExportScope withScope() default ExportScope.GLOBAL;

        CallingConvention callingConvention() default CallingConvention.C;
    }

    /**
     * Declare the annotated native object type as "internal".  Its size and layout will be determined using a
     * default algorithm based on the members of the type.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface internal {
    }

    /**
     * Define and export the annotated method or object as a linkable native symbol. The annotated object or method
     * parameters and return type must have a native type extending {@link object}.
     * <p>
     * Fields annotated with {@link ThreadScoped @ThreadScoped} will be exported as thread locals, however in this
     * case they must not be {@code final}.
     */
    @Target({ ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface export {
        ExportScope withScope() default ExportScope.GLOBAL;

        CallingConvention callingConvention() default CallingConvention.C;
    }

    /**
     * Establish that the annotated function is a global constructor.  The function must be {@linkplain export exported} but
     * may have a {@linkplain ExportScope#LOCAL local export scope}.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface constructor {
        /**
         * The priority in which the constructor is called.  Constructors are called in ascending priority order.
         *
         * @return the priority
         */
        int priority() default 1000;
    }

    /**
     * Establish that the annotated function is a global destructor.  The function must be {@linkplain export exported} but
     * may have a {@linkplain ExportScope#LOCAL local export scope}.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface destructor {
        /**
         * The priority in which the destructor is called.  Destructors are called in descending priority order.
         *
         * @return the priority
         */
        int priority() default 1000;
    }

    /**
     * Indicate that a C type is not modifiable.
     */
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.CLASS)
    public @interface c_const {
    }

    /**
     * Indicate that accesses to objects through the annotated pointer type will <em>only</em> be done through
     * the annotated pointer type. Equivalent to the {@code restrict} type qualifier in C.
     * <p>
     * Only pointers (and thus arrays) may be annotated with this annotation. The following assertions hold:
     * <ul>
     * <li>{@code int * restrict p} in C is equivalent to {@code @restrict ptr<c_int>} in Java
     * <li>{@code const double y[restrict]} in C is equivalent to {@code @c_const c_double @restrict[] y} in Java
     * <li>{@code ptr<@restrict c_int>} will result in a compilation error
     * </ul>
     */
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.CLASS)
    public @interface restrict {
    }

    /**
     * Indicate that each access to an object with the annotated type is considered to be an observable side-effect in the
     * manner
     * of C's {@code volatile} keyword.
     */
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.CLASS)
    public @interface c_volatile {
    }

    /**
     * Specify the size of an array dimension within an array type declaration. Used in cases where an
     * array constructor is not allowed (for example in a structure member).
     * <p>
     * For example: {@code c_char @array_size(12) [] foo = auto(); assert sizeof(foo).intValue() == 12; }
     */
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.CLASS)
    public @interface array_size {
        int value();
    }

    public enum ExportScope {
        LOCAL,
        GLOBAL,
    }

    public enum CallingConvention {
        C,
    }

}
