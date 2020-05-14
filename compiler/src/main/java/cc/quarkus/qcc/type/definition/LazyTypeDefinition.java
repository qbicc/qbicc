package cc.quarkus.qcc.type.definition;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.interpret.InterpreterThread;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.universe.Universe;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class LazyTypeDefinition implements TypeDefinition {
    public LazyTypeDefinition(Universe universe, String name, boolean resolve) {
        this.universe = universe;
        this.name = name;
        if ( resolve ) {
            getResolver();
        }
    }

    private ForkJoinTask<TypeDefinition> getResolver() {
        return this.resolver.updateAndGet( (prev)->{
            if ( prev != null ) {
                return prev;
            }
            return this.universe.getPool().submit( ()-> this.universe.defineClass(name, ByteBuffer.wrap(universe.getClassFinder().findClass(name).readAllBytes())));
        });
    }

    private TypeDefinition getDelegate() {
        return this.delegate.updateAndGet( (prev)->{
            if ( prev != null ) {
                return prev;
            }
            return getResolver().join();
        });
    }

    @Override
    public int getAccess() {
        return getDelegate().getAccess();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public TypeDefinition getSuperclass() {
        return getDelegate().getSuperclass();
    }

    @Override
    public List<TypeDefinition> getInterfaces() {
        return getDelegate().getInterfaces();
    }

    @Override
    public boolean isAssignableFrom(TypeDefinition other) {
        return getDelegate().isAssignableFrom(other);
    }

    @Override
    public TypeDescriptor<ObjectReference> getTypeDescriptor() {
        return getDelegate().getTypeDescriptor();
    }

    @Override
    public Set<MethodDefinition<?>> getMethods() {
        return getDelegate().getMethods();
    }

    @Override
    public MethodDefinition<?> findMethod(String name, String desc) {
        return getDelegate().findMethod(name, desc);
    }

    @Override
    public <V extends QType> MethodDefinition<V> findMethod(MethodDescriptor<V> methodDescriptor) {
        return getDelegate().findMethod(methodDescriptor);
    }

    @Override
    public <V extends QType> FieldDefinition<V> findField(String name) {
        return getDelegate().findField(name);
    }

    @Override
    public <V extends QType> void putField(FieldDefinition<V> field, ObjectReference objRef, V val) {
        getDelegate().putField(field, objRef, val);
    }

    @Override
    public <V extends QType> V getStatic(FieldDefinition<V> field) {
        return getDelegate().getStatic(field);
    }

    @Override
    public <V extends QType> V getField(FieldDefinition<V> field, ObjectReference objRef) {
        return getDelegate().getField(field, objRef);
    }

    @Override
    public ObjectReference newInstance(InterpreterThread thread, QType... arguments) {
        return getDelegate().newInstance(thread, arguments);
    }

    @Override
    public ObjectReference newInstance(InterpreterThread thread, List<QType> arguments) {
        return getDelegate().newInstance(thread, arguments);
    }

    @Override
    public boolean equals(Object obj) {
        return getDelegate().equals(obj);
    }

    @Override
    public String toString() {
        return this.name;
    }

    private final Universe universe;

    private final String name;
    private AtomicReference<ForkJoinTask<TypeDefinition>> resolver = new AtomicReference<>();
    private AtomicReference<TypeDefinition> delegate = new AtomicReference<>();

}
