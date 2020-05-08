package cc.quarkus.qcc.type;

import java.util.List;

public interface MethodDescriptor<V> {

    String getDescriptor();

    TypeDefinition getOwner();

    String getName();

    boolean isStatic();

    List<TypeDescriptor<?>> getParamTypes();

    TypeDescriptor<V> getReturnType();
}
