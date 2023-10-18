package org.qbicc.type.definition.element;

import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.InvokableType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.generic.MethodSignature;

/**
 *
 */
public interface ExecutableElement extends MemberElement {
    /**
     * Determine whether this element has a body factory.  An element with a body factory will
     * produce a body when {@link #tryCreateMethodBody()} is called.
     *
     * @return {@code true} if there is a body factory, {@code false} otherwise
     */
    boolean hasMethodBodyFactory();

    /**
     * Determine whether a body was produced for this element.
     *
     * @return {@code true} if a body was produced, {@code false} otherwise
     */
    boolean hasMethodBody();

    MethodBody getPreviousMethodBody();

    /**
     * Get the executable body.  If no body was created, or the method has none, an exception is thrown.
     *
     * @return the executable body (not {@code null})
     * @throws IllegalStateException if this executable has no body
     */
    MethodBody getMethodBody() throws IllegalStateException;

    /**
     * Attempt to initiate the creation of the method body if it has not yet been initiated.
     * This method is safe to call from a reentrant context.
     *
     * @return {@code true} if the element has a method body; {@code false} otherwise
     */
    boolean tryCreateMethodBody();

    void replaceMethodBody(MethodBody replacement);

    InvokableType getType();

    default <T extends InvokableType> T getType(Class<T> expected) {
        return expected.cast(getType());
    }

    MethodDescriptor getDescriptor();

    MethodSignature getSignature();

    int getMinimumLineNumber();
    int getMaximumLineNumber();

    SafePointBehavior safePointBehavior();

    int safePointSetBits();

    int safePointClearBits();

    interface Builder extends MemberElement.Builder {

        void setMethodBodyFactory(MethodBodyFactory factory, int index);

        ExecutableElement build();

        interface Delegating extends MemberElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            default void setMethodBodyFactory(MethodBodyFactory factory, int index) {
                getDelegate().setMethodBodyFactory(factory, index);
            }

            @Override
            default ExecutableElement build() {
                return getDelegate().build();
            }
        }
    }
}
