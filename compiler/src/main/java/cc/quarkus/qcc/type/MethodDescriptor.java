package cc.quarkus.qcc.type;

import java.util.List;

public interface MethodDescriptor {
    TypeDefinition getOwner();

    String getName();

    boolean isStatic();

    List<TypeDescriptor<?>> getParamTypes();

    TypeDescriptor<?> getReturnType();
}
