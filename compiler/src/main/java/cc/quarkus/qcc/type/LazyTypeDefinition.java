package cc.quarkus.qcc.type;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;

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
    public Set<MethodDefinition> getMethods() {
        return getDelegate().getMethods();
    }

    @Override
    public MethodDefinition getMethod(String name, String desc) {
        return getDelegate().getMethod(name, desc);
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
