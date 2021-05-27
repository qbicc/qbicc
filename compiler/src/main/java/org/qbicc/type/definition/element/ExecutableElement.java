package org.qbicc.type.definition.element;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.generic.MethodSignature;

/**
 *
 */
public interface ExecutableElement extends MemberElement {
    boolean hasMethodBody();

    MethodBody getPreviousMethodBody();

    MethodBody getMethodBody();

    /**
     * Get the executable body, or create it if it has not yet been created.  Note that this method
     * should only be called from the top level of compilation and not reentered unless it is guaranteed that
     * the method body has already been created.
     *
     * @return the method body (must not be {@code null})
     */
    MethodBody getOrCreateMethodBody();

    /**
     * Attempt to initiate the creation of the method body if it has not yet been initiated.  Unlike
     * {@link #getOrCreateMethodBody()}, this method is safe to call from a reentrant context.
     */
    void tryCreateMethodBody();

    void replaceMethodBody(MethodBody replacement);

    FunctionType getType();

    MethodDescriptor getDescriptor();

    MethodSignature getSignature();

    int getMinimumLineNumber();
    int getMaximumLineNumber();

    interface Builder extends Element.Builder {

        void setMethodBodyFactory(MethodBodyFactory factory, int index);

        ExecutableElement build();
    }
}
