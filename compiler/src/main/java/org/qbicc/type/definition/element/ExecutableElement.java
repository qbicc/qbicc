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
