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

    MethodBody getOrCreateMethodBody();

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
