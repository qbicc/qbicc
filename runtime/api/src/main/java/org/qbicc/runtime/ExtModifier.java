package org.qbicc.runtime;

/**
 * Extended modifiers for classes, members, and variables.
 */
public final class ExtModifier {
    private ExtModifier() {}

    public static final int ACC_PUBLIC = 1 << 0;
    public static final int ACC_PRIVATE = 1 << 1;
    public static final int ACC_PROTECTED = 1 << 2;
    public static final int ACC_STATIC = 1 << 3;
    public static final int ACC_FINAL = 1 << 4;
    public static final int ACC_OPEN = 1 << 4; // same as ACC_FINAL
    public static final int ACC_SYNCHRONIZED = 1 << 5;
    public static final int ACC_SUPER = 1 << 5; // same as ACC_SYNCHRONIZED
    public static final int ACC_BRIDGE = 1 << 6;
    public static final int ACC_VOLATILE = 1 << 6; // same as ACC_BRIDGE
    public static final int ACC_STATIC_PHASE = 1 << 6; // same as ACC_BRIDGE
    public static final int ACC_VARARGS = 1 << 7;
    public static final int ACC_TRANSIENT = 1 << 7; // same as ACC_VARARGS
    public static final int ACC_NATIVE = 1 << 8;
    public static final int ACC_INTERFACE = 1 << 9;
    public static final int ACC_ABSTRACT = 1 << 10;
    public static final int ACC_STRICT = 1 << 11;
    public static final int ACC_SYNTHETIC = 1 << 12;
    public static final int ACC_ANNOTATION = 1 << 13;
    public static final int ACC_ENUM = 1 << 14;
    public static final int ACC_MODULE = 1 << 15;
    public static final int ACC_MANDATED = 1 << 15; // same as ACC_MODULE

    /**
     * Quoting from OpenJDK:
     * <p>
     * A signature-polymorphic method (JLS 15.12.3) is a method that
     * <ol type="i">
     *     <li>is declared in the java.lang.invoke.MethodHandle/VarHandle classes;</li>
     *     <li>takes a single variable arity parameter;</li>
     *     <li>whose declared type is {@code Object[]};</li>
     *     <li>has any return type, {@code Object} signifying a polymorphic return type; and</li>
     *     <li>is native.</li>
     * </ol>
     */
    public static final int I_ACC_SIGNATURE_POLYMORPHIC = 1 << 16;
    /**
     * For fields which are declared as {@code final} but are actually mutable, including:
     * <ul>
     *     <li>{@code System.in}/{@code .out}/{@code .err}</li>
     *     <li>fields that are reflected upon or mutated via {@code Unsafe}</li>
     * </ul>
     */
    public static final int I_ACC_NOT_REALLY_FINAL = 1 << 16;
    /*
     * Bit 16 not used for classes.
     */
    // reserved = 1 << 16;
    /**
     * For classes which represent value-based (aka primitive) types.
     */
    public static final int I_ACC_PRIMITIVE = 1 << 17;
    // reserved = 1 << 17; (methods)
    /**
     * On methods, hide from stack traces.  On classes, defined as a JEP 371 "hidden class".
     */
    public static final int I_ACC_HIDDEN = 1 << 18;
    /**
     * For static fields that are thread-local.
     */
    public static final int I_ACC_THREAD_LOCAL = 1 << 19;
    public static final int I_ACC_ALWAYS_INLINE = 1 << 20;
    public static final int I_ACC_NEVER_INLINE = 1 << 21;
    /**
     * For methods which have no side effects.
     */
    public static final int I_ACC_NO_SIDE_EFFECTS = 1 << 22;
    /**
     * For members and types which should not appear to reflection.
     */
    public static final int I_ACC_NO_REFLECT = 1 << 23;
    /**
     * For members which should never be symbolically resolvable and classes that should not be registered to the class loader.
     */
    public static final int I_ACC_NO_RESOLVE = 1 << 24;
    /**
     * For executable members which never return normally.
     */
    public static final int I_ACC_NO_RETURN = 1 << 25;
    /**
     * For executable members which never throw - not even {@link StackOverflowError} or {@link OutOfMemoryError}.
     */
    public static final int I_ACC_NO_THROW = 1 << 26;
    /**
     * For executable members which should be evaluated during compilation.
     */
    public static final int I_ACC_FOLD = 1 << 27;
    /**
     * For members which are visible at run time.  Members with this annotation are not accessible during build.
     * Fields which are available at both build time and run time do <em>not</em> have this modifier, even if they
     * are associated with an initializer that does have this modifier.
     */
    public static final int I_ACC_RUN_TIME = 1 << 28;
    /**
     * For methods which have the JDK {@code @CallerSensitive} annotation.
     */
    public static final int I_ACC_CALLER_SENSITIVE = 1 << 29;
    /**
     * For methods which are only invokable at build time.  Members with this annotation are not invokable during runtime.
     */
    public static final int I_ACC_BUILD_TIME_ONLY = 1 << 30;
    /**
     * For classes. Indicates that the GC bitmap is a pointer to the full bitmap, rather than a {@code long} value.
     */
    public static final int I_ACC_EXTENDED_BITMAP = 1 << 31;
}
