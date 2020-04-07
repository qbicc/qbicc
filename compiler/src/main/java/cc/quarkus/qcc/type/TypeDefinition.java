package cc.quarkus.qcc.type;

import java.util.List;
import java.util.Set;

public interface TypeDefinition {
    int getAccess();

    String getName();

    TypeDefinition getSuperclass();

    List<TypeDefinition> getInterfaces();

    Set<MethodDefinition> getMethods();

    MethodDefinition getMethod(String name, String desc);
}
