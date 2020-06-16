package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

/**
 *
 */
public interface ResolvedMethodDefinition extends DefinedMethodDefinition {

    default ResolvedMethodDefinition resolve() {
        return this;
    }

    MethodIdentifier getMethodIdentifier();

    Type getParameterType(int index) throws IndexOutOfBoundsException;

    Type getReturnType();
}
