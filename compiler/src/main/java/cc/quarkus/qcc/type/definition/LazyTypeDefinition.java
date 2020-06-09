package cc.quarkus.qcc.type.definition;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.universe.Universe;

public class LazyTypeDefinition implements TypeDefinition {
    public LazyTypeDefinition(Universe universe, String name) {
        this.universe = universe;
        this.name = name;
    }

    public TypeDefinition getDelegate() {
        TypeDefinition delegate = this.delegate;
        if (delegate == null) {
            synchronized (this) {
                delegate = this.delegate;
                if (delegate == null) {
                    try {
                        delegate = resolve();
                    } catch (IOException | ClassNotFoundException e) {
                        delegate = new UnresolvableClassDefinition(name);
                    }
                    this.delegate = delegate;
                }
            }
        }
        return delegate;
    }

    private TypeDefinition resolve() throws IOException, ClassNotFoundException {
        return this.universe.defineClass(name, ByteBuffer.wrap(universe.getClassFinder().findClass(name).readAllBytes()));
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
    public ClassType getType() {
        ClassType cachedType = this.cachedType;
        if (cachedType == null) {
            synchronized (this) {
                cachedType = this.cachedType;
                if (cachedType == null) {
                    this.cachedType = cachedType = Type.classNamed(name);
                }
            }
        }
        return cachedType;
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
    public <V> MethodDefinition<V> findMethod(MethodDescriptor methodDescriptor) {
        return getDelegate().findMethod(methodDescriptor);
    }

    @Override
    public <V> FieldDefinition<V> findField(String name) {
        return getDelegate().findField(name);
    }

    @Override
    public <V> void putField(FieldDefinition<V> field, ObjectReference objRef, V val) {
        getDelegate().putField(field, objRef, val);
    }

    @Override
    public <V> V getStatic(FieldDefinition<V> field) {
        return getDelegate().getStatic(field);
    }

    @Override
    public <V> V getField(FieldDefinition<V> field, ObjectReference objRef) {
        return getDelegate().getField(field, objRef);
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
    private volatile TypeDefinition delegate;
    private volatile ClassType cachedType;
}
