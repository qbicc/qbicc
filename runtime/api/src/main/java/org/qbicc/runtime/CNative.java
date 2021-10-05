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
import java.util.function.BooleanSupplier;
import java.util.function.UnaryOperator;

import org.qbicc.runtime.stdc.Stdlib;

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
    public static native <T extends object, P extends ptr<T>> P addr_of(T obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array, or a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native int64_t_ptr addr_of(long obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native int32_t_ptr addr_of(int obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native int16_t_ptr addr_of(short obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native int8_t_ptr addr_of(byte obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native uint16_t_ptr addr_of(char obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native _Bool_ptr addr_of(boolean obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native _Float32_ptr addr_of(float obj);

    /**
     * Get the address of the given value.  The value may be a local variable, or it may be a member or element of a
     * {@linkplain Pin pinned} heap object or array of a {@linkplain StackObject stack object} or array.  The value
     * <em>must not</em> be a member or element of an unpinned heap object or array.
     *
     * @param obj the value to get the address of
     * @return a pointer to the native representation of the value
     */
    public static native _Float64_ptr addr_of(double obj);

    /**
     * Get a static method reference as a C function pointer. The function pointer will have normal prologue/epilogue.
     * The given function object is constant-folded, thus it must not capture any run time state, otherwise compilation
     * failure will result. A method handle to a static method is usually an appropriate value type.
     *
     * @param function the function instance (e.g. method handle)
     * @param <F> the function interface type
     * @return the function pointer
     */
    public static native <F> function_ptr<F> addr_of_function(F function);

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

    // TODO pointer casting temporary workaround
    public static native ptr<?> castPtr(ptr<?> obj, Class<?> ptrClass);


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

    public static int8_t wordExact(byte byteValue) {
        return word(byteValue);
    }

    public static int16_t wordExact(short shortValue) {
        return word(shortValue);
    }

    public static int32_t wordExact(int intValue) {
        return word(intValue);
    }

    public static int64_t wordExact(long longValue) {
        return word(longValue);
    }

    public static uint16_t wordExact(char charValue) {
        return word(charValue);
    }

    public static _Bool wordExact(boolean booleanValue) {
        return word(booleanValue);
    }

    public static _Float32 wordExact(float floatValue) {
        return word(floatValue);
    }

    public static _Float64 wordExact(double doubleValue) {
        return word(doubleValue);
    }

    public static void copy(int8_t_ptr dest, byte[] src, int srcOff, int len) {
        memcpy(dest.cast(), addr_of(src[srcOff]).cast(), word((long)len));
    }

    public static native void copy(int16_t_ptr dest, short[] src, int srcOff, int len);

    public static native void copy(int32_t_ptr dest, int[] src, int srcOff, int len);

    public static native void copy(int64_t_ptr dest, long[] src, int srcOff, int len);

    public static native void copy(uint16_t_ptr dest, char[] src, int srcOff, int len);

    public static void copy(byte[] dest, int destOff, int destLen, const_int8_t_ptr src) {
        memcpy(addr_of(dest[destOff]).cast(), src.cast(), word((long)destLen));
    }

    public static native void copy(short[] dest, int destOff, int destLen, const_int16_t_ptr src);

    public static native void copy(int[] dest, int destOff, int destLen, const_int32_t_ptr src);

    public static native void copy(long[] dest, int destOff, int destLen, const_int64_t_ptr src);

    public static native void copy(char[] dest, int destOff, int destLen, const_uint16_t_ptr src);

    public static String utf8zToJavaString(const_uint8_t_ptr ptr) {
        return utf8ToJavaString(ptr, strlen(ptr.cast()).intValue());
    }

    public static String utf8ToJavaString(const_uint8_t_ptr ptr, int len) {
        final byte[] bytes = new byte[len];
        copy(bytes, 0, len, ptr.cast());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String asciizToJavaString(const_int8_t_ptr ptr) {
        return asciiToJavaString(ptr, strlen(ptr.cast()).intValue());
    }

    public static String asciiToJavaString(const_int8_t_ptr ptr, int len) {
        final byte[] bytes = new byte[len];
        copy(bytes, 0, len, ptr);
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
    public static native Object ptrToRef(ptr<?> ptr);

    /**
     * <b>Warning:</b> this is a potentially dangerous operation and should only be performed by internal components.
     * Directly convert a reference to a pointer.
     *
     * @param ref the reference (may be {@code null})
     * @return the pointer (may be {@code null})
     */
    public static native <P extends ptr<?>> P refToPtr(Object ref);

    // built-in

    /**
     * Allocate some bytes on the stack and return a pointer. To create an automatic-duration object directly,
     * use {@link #zero} or {@link #auto()}, or use an {@link object} subclass array constructor.
     *
     * @param size the size to allocate
     * @return the object
     */
    public static native <P extends ptr<?>> P alloca(size_t size);

    // thread attach

    /**
     * Attach to a thread which is created and started.  Only call if there is no currently attached thread.
     *
     * @param name the name of the new thread
     * @param threadGroup the thread group to use
     */
    public static native void attachNewThread(String name, ThreadGroup threadGroup);

    /**
     * Reattach to the given running thread (used for native-to-Java call-ins).  The thread must be reattached from
     * the same system thread or the resultant behavior will be undefined.
     *
     * @param thread the running thread to reattach to
     */
    public static native void reattach(Thread thread);

    /**
     * A native object. Native objects are allocated on the stack or in the system heap, or are otherwise externally
     * managed.
     * <p>
     * Native object types may be <em>complete</em> or <em>incomplete</em>. An object of a complete type may have its
     * size checked via {@link #sizeof(object)}, or be allocated on the stack or system heap using {@link #auto()},
     * {@link #zero}, {@link Stdlib#malloc(Class)}, etc., and pointers to objects of complete type may be dereferenced.
     * An object of incomplete type may not do any of these things; however, it is still allowed to pass around pointers
     * to objects of incomplete type.
     * <p>
     * Arrays of native objects are allocated on the stack when the standard Java array constructor is used.
     */
    public abstract static class object extends InlineObject implements AutoCloseable {
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
         * In these cases, the type must be specified, for example: {@code var result = obj.<int32_t_ptr>cast();}
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

        /**
         * Release the object. Not every object can be released, but objects
         * with automatic storage duration may be considered released after
         * this method is called, as a hint to the compiler.
         */
        public native final void close();

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

        public final float floatValue() {
            return Float.intBitsToFloat(intValue());
        }

        public native final short shortValue();

        public native final byte byteValue();

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
    }

    /**
     * The special type which corresponds to a value whose type is a run time type ID.
     */
    public static final class type_id extends word {
    }

    /**
     * The special type representing the platform-specific variable argument list.
     */
    @include("<stdarg.h>")
    public static final class va_list extends object {
    }

    /**
     * Start the variable argument processing.  May only be called from methods which have a final
     * variadic argument of type {@code object...}.
     *
     * @param ap the list to initialize
     */
    public static native void va_start(va_list ap);
    public static native <T extends object> T va_arg(va_list ap, Class<T> type);
    public static native void va_end(va_list ap);
    public static native void va_copy(va_list dest, va_list src);

    // basic types

    @name("char")
    @size(1) // by definition
    public static final class c_char extends word {}

    public static final class char_ptr extends ptr<c_char> {}
    public static final class const_char_ptr extends ptr<@c_const c_char> {}
    public static final class char_ptr_ptr extends ptr<char_ptr> {}
    public static final class const_char_ptr_ptr extends ptr<const_char_ptr> {}
    public static final class char_ptr_const_ptr extends ptr<@c_const char_ptr> {}
    public static final class const_char_ptr_const_ptr extends ptr<@c_const const_char_ptr> {}

    public static final class _Bool extends word {}

    public static final class _Bool_ptr extends ptr<_Bool> {}
    public static final class const__Bool_ptr extends ptr<@c_const _Bool> {}
    public static final class _Bool_ptr_ptr extends ptr<_Bool_ptr> {}
    public static final class const__Bool_ptr_ptr extends ptr<const__Bool_ptr> {}
    public static final class _Bool_ptr_const_ptr extends ptr<@c_const _Bool_ptr> {}
    public static final class const__Bool_ptr_const_ptr extends ptr<@c_const const__Bool_ptr> {}

    // basic signed types

    @name("signed char")
    @size(1) // by definition
    @signed(true) // by definition
    public static final class signed_char extends word {}

    public static final class signed_char_ptr extends ptr<signed_char> {}
    public static final class const_signed_char_ptr extends ptr<@c_const signed_char> {}
    public static final class signed_char_ptr_ptr extends ptr<signed_char_ptr> {}
    public static final class const_signed_char_ptr_ptr extends ptr<const_signed_char_ptr> {}
    public static final class signed_char_ptr_const_ptr extends ptr<@c_const signed_char_ptr> {}
    public static final class const_signed_char_ptr_const_ptr extends ptr<@c_const const_signed_char_ptr> {}


    @name("short")
    @signed(true) // by definition
    public static final class c_short extends word {}

    public static final class short_ptr extends ptr<c_short> {}
    public static final class const_short_ptr extends ptr<@c_const c_short> {}
    public static final class short_ptr_ptr extends ptr<short_ptr> {}
    public static final class const_short_ptr_ptr extends ptr<const_short_ptr> {}
    public static final class short_ptr_const_ptr extends ptr<@c_const short_ptr> {}
    public static final class const_short_ptr_const_ptr extends ptr<@c_const const_short_ptr> {}

    @name("int")
    @signed(true) // by definition
    public static final class c_int extends word {}

    public static final class int_ptr extends ptr<c_int> {}
    public static final class const_int_ptr extends ptr<@c_const c_int> {}
    public static final class int_ptr_ptr extends ptr<int_ptr> {}
    public static final class const_int_ptr_ptr extends ptr<const_int_ptr> {}
    public static final class int_ptr_const_ptr extends ptr<@c_const int_ptr> {}
    public static final class const_int_ptr_const_ptr extends ptr<@c_const const_int_ptr> {}

    @name("long")
    @signed(true) // by definition
    public static final class c_long extends word {}

    public static final class long_ptr extends ptr<c_long> {}
    public static final class const_long_ptr extends ptr<@c_const c_long> {}
    public static final class long_ptr_ptr extends ptr<long_ptr> {}
    public static final class const_long_ptr_ptr extends ptr<const_long_ptr> {}
    public static final class long_ptr_const_ptr extends ptr<@c_const long_ptr> {}
    public static final class const_long_ptr_const_ptr extends ptr<@c_const const_long_ptr> {}

    @name("long long")
    @signed(true) // by definition
    public static final class long_long extends word {}

    public static final class long_long_ptr extends ptr<long_long> {}
    public static final class const_long_long_ptr extends ptr<@c_const long_long> {}
    public static final class long_long_ptr_ptr extends ptr<long_long_ptr> {}
    public static final class const_long_long_ptr_ptr extends ptr<const_long_long_ptr> {}
    public static final class long_long_ptr_const_ptr extends ptr<@c_const long_long_ptr> {}
    public static final class const_long_long_ptr_const_ptr extends ptr<@c_const const_long_long_ptr> {}

    // basic unsigned types

    @name("unsigned char")
    @size(1) // by definition
    @signed(false) // by definition
    public static final class unsigned_char extends word {}

    public static final class unsigned_char_ptr extends ptr<unsigned_char> {}
    public static final class const_unsigned_char_ptr extends ptr<@c_const unsigned_char> {}
    public static final class unsigned_char_ptr_ptr extends ptr<unsigned_char_ptr> {}
    public static final class const_unsigned_char_ptr_ptr extends ptr<const_unsigned_char_ptr> {}
    public static final class unsigned_char_ptr_const_ptr extends ptr<@c_const unsigned_char_ptr> {}
    public static final class const_unsigned_char_ptr_const_ptr extends ptr<@c_const const_unsigned_char_ptr> {}


    @name("unsigned short")
    @signed(false) // by definition
    public static final class unsigned_short extends word {}

    public static final class unsigned_short_ptr extends ptr<unsigned_short> {}
    public static final class const_unsigned_short_ptr extends ptr<@c_const unsigned_short> {}
    public static final class unsigned_short_ptr_ptr extends ptr<unsigned_short_ptr> {}
    public static final class const_unsigned_short_ptr_ptr extends ptr<const_unsigned_short_ptr> {}
    public static final class unsigned_short_ptr_const_ptr extends ptr<@c_const unsigned_short_ptr> {}
    public static final class const_unsigned_short_ptr_const_ptr extends ptr<@c_const const_unsigned_short_ptr> {}

    @name("unsigned int")
    @signed(false) // by definition
    public static final class unsigned_int extends word {}

    public static final class unsigned_int_ptr extends ptr<unsigned_int> {}
    public static final class const_unsigned_int_ptr extends ptr<@c_const unsigned_int> {}
    public static final class unsigned_int_ptr_ptr extends ptr<unsigned_int_ptr> {}
    public static final class const_unsigned_int_ptr_ptr extends ptr<const_unsigned_int_ptr> {}
    public static final class unsigned_int_ptr_const_ptr extends ptr<@c_const unsigned_int_ptr> {}
    public static final class const_unsigned_int_ptr_const_ptr extends ptr<@c_const const_unsigned_int_ptr> {}

    @name("unsigned long")
    @signed(false) // by definition
    public static final class unsigned_long extends word {}

    public static final class unsigned_long_ptr extends ptr<unsigned_long> {}
    public static final class const_unsigned_long_ptr extends ptr<@c_const unsigned_long> {}
    public static final class unsigned_long_ptr_ptr extends ptr<unsigned_long_ptr> {}
    public static final class const_unsigned_long_ptr_ptr extends ptr<const_unsigned_long_ptr> {}
    public static final class unsigned_long_ptr_const_ptr extends ptr<@c_const unsigned_long_ptr> {}
    public static final class const_unsigned_long_ptr_const_ptr extends ptr<@c_const const_unsigned_long_ptr> {}

    @name("unsigned long long")
    @signed(false) // by definition
    public static final class unsigned_long_long extends word {}

    public static final class unsigned_long_long_ptr extends ptr<unsigned_long_long> {}
    public static final class const_unsigned_long_long_ptr extends ptr<@c_const unsigned_long_long> {}
    public static final class unsigned_long_long_ptr_ptr extends ptr<unsigned_long_long_ptr> {}
    public static final class const_unsigned_long_long_ptr_ptr extends ptr<const_unsigned_long_long_ptr> {}
    public static final class unsigned_long_long_ptr_const_ptr extends ptr<@c_const unsigned_long_long_ptr> {}
    public static final class const_unsigned_long_long_ptr_const_ptr extends ptr<@c_const const_unsigned_long_long_ptr> {}

    // basic FP types

    @name("float")
    public static final class c_float extends word {}

    public static final class float_ptr extends ptr<c_float> {}
    public static final class const_float_ptr extends ptr<@c_const c_float> {}
    public static final class float_ptr_ptr extends ptr<float_ptr> {}
    public static final class const_float_ptr_ptr extends ptr<const_float_ptr> {}
    public static final class float_ptr_const_ptr extends ptr<@c_const float_ptr> {}
    public static final class const_float_ptr_const_ptr extends ptr<@c_const const_float_ptr> {}

    @name("double")
    public static final class c_double extends word {}

    public static final class double_ptr extends ptr<c_double> {}
    public static final class const_double_ptr extends ptr<@c_const c_double> {}
    public static final class double_ptr_ptr extends ptr<double_ptr> {}
    public static final class const_double_ptr_ptr extends ptr<const_double_ptr> {}
    public static final class double_ptr_const_ptr extends ptr<@c_const double_ptr> {}
    public static final class const_double_ptr_const_ptr extends ptr<@c_const const_double_ptr> {}


    @name("long double")
    public static final class long_double extends word {}

    public static final class long_double_ptr extends ptr<long_double> {}
    public static final class const_long_double_ptr extends ptr<@c_const long_double> {}
    public static final class long_double_ptr_ptr extends ptr<long_double_ptr> {}
    public static final class const_long_double_ptr_ptr extends ptr<const_long_double_ptr> {}
    public static final class long_double_ptr_const_ptr extends ptr<@c_const long_double_ptr> {}
    public static final class const_long_double_ptr_const_ptr extends ptr<@c_const const_long_double_ptr> {}

    @incomplete
    public static final class c_void extends object {}

    /**
     * A pointer to a location in memory whose type is a C native type.
     *
     * @param <T> the invariant pointer type
     */
    public static abstract class ptr<T extends object> extends word {
        /**
         * Dereference the pointer, returning what the pointer points to. This operation
         * does not necessarily directly translate to a physical memory operation.
         *
         * @return the pointed-to value
         */
        public native T deref();

        /**
         * Overwrite the value that is pointed to by this pointer.
         *
         * @param value the value to write
         */
        public native void derefAssign(T value);

        /**
         * Get an array view of this pointer value. The array points to the same address as
         * this pointer, but has an incomplete type (no size). If the object being pointed to
         * has an incomplete type, using this method will result in a compilation error.
         * <p>
         * Writing to this array has the same effect as calling {@link #set(int, object)} on the corresponding
         * pointer; reading from this array has the same effect as calling {@link #get(int)}.
         * <p>
         * Native arrays have no run time bounds checking.
         *
         * @return the array view
         */
        public native T[] asArray();

        /**
         * Treating this pointer as an array, get the array element at the given index. This is equivalent
         * to {@linkplain #deref()} dereferencing} the pointer at the given {@linkplain #plus(int) offset}.
         * In C, this is equivalent to pointer addition.
         *
         * @param arrayIdx the array index
         * @return the value
         */
        public T get(int arrayIdx) {
            return plus(arrayIdx).deref();
        }

        public void set(int arrayIdx, T newVal) {
            plus(arrayIdx).derefAssign(newVal);
        }

        /**
         * Get a pointer which is offset from the base by the given number of elements.
         *
         * @param offset the element offset
         * @return the offset pointer
         */
        public native <P extends ptr<T>> P plus(int offset);

        /**
         * Get the difference between this pointer and another pointer of the same type.
         *
         * @param other the other pointer
         * @return the difference between the two pointers
         */
        public native ptrdiff_t minus(ptr<T> other);

        /**
         * Get a pointer to an arbitrary type which is offset from the base by the given signed difference.
         *
         * @param offset the pointer offset
         * @return the offset pointer
         */
        public native <R extends object, P extends ptr<R>> P plus(ptrdiff_t offset);

        /**
         * Get a pointer to an arbitrary type which is offset from the base by the given unsigned offset.
         *
         * @param offset the pointer offset
         * @return the offset pointer
         */
        public native <R extends object, P extends ptr<R>> P plus(size_t offset);

        /**
         * Get a pointer to an arbitrary type which is offset from the base by the negation of the given signed difference.
         *
         * @param offset the pointer offset
         * @return the offset pointer
         */
        public native <R extends object, P extends ptr<R>> P minus(ptrdiff_t offset);

        /**
         * Get a pointer to an arbitrary type which is offset from the base by the negation of the given unsigned offset.
         *
         * @param offset the pointer offset
         * @return the offset pointer
         */
        public native <R extends object, P extends ptr<R>> P minus(size_t offset);

        /**
         * Determine if this pointer instance is a pointer to a pinned Java data structure.
         *
         * @return {@code true} if this is a pointer to a pinned Java data structure, {@code false} if it is not or
         *         it cannot be determined
         */
        public native boolean isPinned();
    }

    public static final class void_ptr extends ptr<c_void> {}
    public static final class void_ptr_ptr extends ptr<void_ptr> {}
    public static final class const_void_ptr extends ptr<@c_const c_void> {}
    public static final class const_void_ptr_ptr extends ptr<const_void_ptr> {}

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
        public native F getInvokable();
    }

    public static final class void_ptr_unaryoperator_function_ptr extends ptr<function<UnaryOperator<void_ptr>>> {}
    public static final class function_ptr<F> extends ptr<function<F>> {}

    // floating point

    // todo: revisit this with a more sophisticated probe
    @name("float")
    public static final class _Float32 extends word {}

    public static final class _Float32_ptr extends ptr<_Float32> {}
    public static final class const__Float32_ptr extends ptr<@c_const _Float32> {}
    public static final class _Float32_ptr_ptr extends ptr<_Float32_ptr> {}
    public static final class const__Float32_ptr_ptr extends ptr<const__Float32_ptr> {}
    public static final class _Float32_ptr_const_ptr extends ptr<@c_const _Float32_ptr> {}
    public static final class const__Float32_ptr_const_ptr extends ptr<@c_const const__Float32_ptr> {}

    // todo: revisit this with a more sophisticated probe
    @name("double")
    public static final class _Float64 extends word {}

    public static final class _Float64_ptr extends ptr<_Float64> {}
    public static final class const__Float64_ptr extends ptr<@c_const _Float64> {}
    public static final class _Float64_ptr_ptr extends ptr<_Float64_ptr> {}
    public static final class const__Float64_ptr_ptr extends ptr<const__Float64_ptr> {}
    public static final class _Float64_ptr_const_ptr extends ptr<@c_const _Float64_ptr> {}
    public static final class const__Float64_ptr_const_ptr extends ptr<@c_const const__Float64_ptr> {}

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

    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface name {
        String value();
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
        String withName() default "";

        ExportScope withScope() default ExportScope.GLOBAL;

        CallingConvention callingConvention() default CallingConvention.C;
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
