package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.universe.Universe;

public class UnresolvableClassDefinition implements TypeDefinition {

    public UnresolvableClassDefinition(String name, final Universe universe) {
        this.name = name;
        this.universe = universe;
    }

    protected RuntimeException unresolved() {
        throw new RuntimeException("Class " + this.name + " is unresolved");
    }

    @Override
    public MethodDefinition findMethod(String name, String desc) {
        throw unresolved();
    }

    @Override
    public MethodDefinition findMethod(MethodDescriptor methodDescriptor) {
        throw unresolved();
    }

    @Override
    public MethodDefinition resolveMethod(MethodDescriptor methodDescriptor) {
        throw unresolved();
    }

    @Override
    public MethodDefinition resolveInterfaceMethod(MethodDescriptor methodDescriptor) {
        throw unresolved();
    }

    @Override
    public MethodDefinition resolveInterfaceMethod(MethodDescriptor methodDescriptor, boolean searchingSuper) {
        throw unresolved();
    }

    @Override
    public List<FieldDefinition> getFields() {
        throw unresolved();
    }

    @Override
    public FieldDefinition resolveField(FieldDescriptor fieldDescriptor) {
        throw unresolved();
    }

    @Override
    public FieldDefinition findField(String name) {
        throw unresolved();
    }

    @Override
    public Object getStatic(FieldDefinition field) {
        throw unresolved();
    }

    @Override
    public Object getField(FieldDefinition field, ObjectReference objRef) {
        throw unresolved();
    }

    @Override
    public void putField(FieldDefinition field, ObjectReference objRef, Object val) {
        throw unresolved();
    }

    public ClassType getType() {
        throw unresolved();
    }

    @Override
    public boolean isAssignableFrom(TypeDefinition other) {
        throw unresolved();
    }

    @Override
    public Universe getUniverse() {
        return universe;
    }

    @Override
    public int getAccess() {
        throw unresolved();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public TypeDefinition getSuperclass() {
        throw unresolved();
    }

    @Override
    public List<TypeDefinition> getInterfaces() {
        throw unresolved();
    }

    @Override
    public List<MethodDefinition> getMethods() {
        throw unresolved();
    }

    private final String name;
    private final Universe universe;
}
