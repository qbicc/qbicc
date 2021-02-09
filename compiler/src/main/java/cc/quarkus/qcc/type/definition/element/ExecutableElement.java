package cc.quarkus.qcc.type.definition.element;

import java.util.List;

import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodBodyFactory;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;

/**
 *
 */
public interface ExecutableElement extends MemberElement {
    boolean hasMethodBody();

    MethodBody getPreviousMethodBody();

    MethodBody getMethodBody();

    MethodBody getOrCreateMethodBody();

    void replaceMethodBody(MethodBody replacement);

    FunctionType getType(List<ParameterizedSignature> signatureContext);

    MethodDescriptor getDescriptor();

    MethodSignature getSignature();

    int getMinimumLineNumber();
    int getMaximumLineNumber();

    interface Builder extends Element.Builder {

        void setMethodBodyFactory(MethodBodyFactory factory, int index);

        ExecutableElement build();
    }
}
