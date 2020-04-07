package cc.quarkus.qcc.type;

import java.util.List;
import java.util.Set;

public class UnresolvableClassDefinition implements TypeDefinition {

    public UnresolvableClassDefinition(String name) {
        this.name = name;
    }


    @Override
    public MethodDefinition getMethod(String name, String desc) {
        throw new RuntimeException("Class " + this.name + " is unresolved");
        //return null;
    }

    @Override
    public int getAccess() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public TypeDefinition getSuperclass() {
        return null;
    }

    @Override
    public List<TypeDefinition> getInterfaces() {
        return null;
    }

    @Override
    public Set<MethodDefinition> getMethods() {
        return null;
    }

    private final String name;
}
