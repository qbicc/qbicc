package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.type.definition.TypeDefinition;

public interface MethodDescriptor<V> {

    String getDescriptor();

    TypeDefinition getOwner();

    String getName();

    boolean isStatic();

    List<TypeDescriptor<?>> getParamTypes();

    TypeDescriptor<V> getReturnType();
}
