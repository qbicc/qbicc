package org.qbicc.interpreter;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.qbicc.pointer.Pointer;

/**
 * Indicate that a method is a build time hook for a specific API method.
 * The hook method must have a signature which is compatible with the
 * method being targeted for replacement.
 * <p>
 * The hook method may be a {@code static} method or an instance method.
 * <p>
 * All hook methods must accept a {@link VmThread} as their first argument.
 * Hook methods that intercept instance methods must additionally accept the receiver
 * as their first argument; the type of the receiver must be determined according
 * to the type equivalencies given below.
 * <p>
 * A hook method may optionally throw an exception with a type of {@link Thrown}.
 * This exception must contain a non-{@code null} object of type {@link VmThrowable} which
 * contains the actual exception payload.
 * Any other exception or error thrown by the hook method may cause the interpreter to abort.
 * <p>
 * For a given argument or return type of the original method, the hook method should use these
 * equivalent types:
 * <ul>
 *     <li>Any primitive type including {@code void} but excluding {@code long} - the same primitive type</li>
 *     <li>{@code java.lang.String} - either {@link VmString} or {@link VmObject}</li>
 *     <li>{@code java.lang.Class} - either {@link VmClass} or {@link VmObject}</li>
 *     <li>{@code java.lang.ClassLoader} or any subclass thereof - either {@link VmClassLoader} or {@link VmObject}</li>
 *     <li>{@code java.lang.Thread} or any subclass thereof - either {@link VmThread} or {@link VmObject}</li>
 *     <li>{@code java.lang.Throwable} or any subclass thereof - either {@link VmThrowable} or {@link VmObject}</li>
 *     <li>An array of any primitive type - {@link VmArray} or {@link VmObject}</li>
 *     <li>An array of reference type - {@link VmReferenceArray}, {@link VmArray}, or {@link VmObject}</li>
 *     <li>Any scalar native type - an equivalently-sized primitive type</li>
 *     <li>Any native pointer type, or Java {@code long} - {@link Pointer}</li>
 *     <li>Any compound native type including native arrays or structures - {@link Memory}</li>
 * </ul>
 * <p>
 * In certain limited circumstances, subtypes of these types may be given instead, however any
 * mismatch will trigger unexpected class cast exceptions so care should be taken.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Hook {
    /**
     * The name of the method to intercept.
     * If not given, the hook method's name is used.
     *
     * @return the name of the method to intercept
     */
    String name() default "";

    /**
     * The descriptor of the method to intercept.
     * If not given, then there must be only one method with a matching name.
     *
     * @return the descriptor
     */
    String descriptor() default "";
}
