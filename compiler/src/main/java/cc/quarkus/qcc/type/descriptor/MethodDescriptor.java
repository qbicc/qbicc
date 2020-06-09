package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.TypeDefinition;

public interface MethodDescriptor {

    String getDescriptor();

    TypeDefinition getOwner();

    String getName();

    boolean isStatic();

    List<Type> getParamTypes();

    Type getReturnType();
}
