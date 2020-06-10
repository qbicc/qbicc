package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.graph.ArrayType;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.TypeDefinition;

public class MethodDescriptorImpl implements MethodDescriptor {

    MethodDescriptorImpl(TypeDefinition owner, String name, List<Type> paramTypes, Type returnType, String descriptor, boolean isStatic) {
        this.owner = owner;
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.descriptor = descriptor;
        this.isStatic = isStatic;
    }

    @Override
    public TypeDefinition getOwner() {
        return this.owner;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isStatic() {
        return this.isStatic;
    }

    @Override
    public List<Type> getParamTypes() {
        return this.paramTypes;
    }

    @Override
    public Type getReturnType() {
        return this.returnType;
    }

    @Override
    public String getDescriptor() {
        return this.descriptor;
    }

    @Override
    public boolean matches(MethodDescriptor other) {
        if ( ! this.name.equals(other.getName() ) ) {
            return false;
        }
        if ( this.paramTypes.size() != other.getParamTypes().size()) {
            return false;
        }

        int len = this.paramTypes.size();
        for ( int i = 0 ; i < len ; ++i ) {
            if ( this.paramTypes.get(i) != other.getParamTypes().get(i)) {
                return false;
            }
        }
        return true;
    }



    @Override
    public String toString() {
        return "MethodDescriptorImpl{" +
                "paramTypes=" + paramTypes +
                ", returnType=" + returnType +
                ", owner=" + owner +
                ", name='" + name + '\'' +
                ", isStatic=" + isStatic +
                ", descriptor='" + descriptor + '\'' +
                '}';
    }

    private final List<Type> paramTypes;

    private final Type returnType;

    private final TypeDefinition owner;

    private final String name;

    private final boolean isStatic;

    private final String descriptor;
}
