package cc.quarkus.qcc.type;

import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.interpret.Heap;

public class UnresolvableClassDefinition implements TypeDefinition {

    public UnresolvableClassDefinition(String name) {
        this.name = name;
    }

    protected <R> R throwUnresolved() {
        throw new RuntimeException("Class " + this.name + " is unresolved");
    }

    @Override
    public MethodDefinition<?> findMethod(String name, String desc) {
        return throwUnresolved();
    }

    @Override
    public <V> MethodDefinition<V> findMethod(MethodDescriptor<V> methodDescriptor) {
        return throwUnresolved();
    }

    @Override
    public <V> FieldDefinition<V> findField(String name) {
        return throwUnresolved();
    }

    @Override
    public <V> V getStatic(FieldDefinition<V> field) {
        return throwUnresolved();
    }

    @Override
    public <V> V getField(FieldDefinition<V> field, ObjectReference objRef) {
        return throwUnresolved();
    }

    @Override
    public <V> void putField(FieldDefinition<V> field, ObjectReference objRef, V val) {
        throwUnresolved();
    }

    @Override
    public TypeDescriptor<ObjectReference> getTypeDescriptor() {
        return throwUnresolved();
    }

    @Override
    public ObjectReference newInstance(Heap heap, Object... arguments) {
        return throwUnresolved();
    }

    @Override
    public ObjectReference newInstance(Heap heap, List<Object> arguments) {
        return throwUnresolved();
    }

    @Override
    public boolean isAssignableFrom(TypeDefinition other) {
        return throwUnresolved();
    }

    @Override
    public int getAccess() {
        return throwUnresolved();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public TypeDefinition getSuperclass() {
        return throwUnresolved();
    }

    @Override
    public List<TypeDefinition> getInterfaces() {
        return throwUnresolved();
    }

    @Override
    public Set<MethodDefinition<?>> getMethods() {
        return throwUnresolved();
    }

    private final String name;
}
