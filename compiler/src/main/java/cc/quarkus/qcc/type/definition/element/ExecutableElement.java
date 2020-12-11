package cc.quarkus.qcc.type.definition.element;

import java.util.List;

import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;

/**
 *
 */
public interface ExecutableElement extends MemberElement {
    boolean hasMethodBody();

    MethodHandle getMethodBody();

    FunctionType getType(ClassContext classContext, List<ParameterizedSignature> signatureContext);

    MethodDescriptor getDescriptor();

    MethodSignature getSignature();

    interface Builder extends Element.Builder {
        void setMethodBody(MethodHandle methodHandle);

        ExecutableElement build();
    }
}
