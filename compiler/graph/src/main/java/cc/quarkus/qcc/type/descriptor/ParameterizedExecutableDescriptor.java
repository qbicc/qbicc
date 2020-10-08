package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.graph.Type;

/**
 *
 */
public interface ParameterizedExecutableDescriptor {
    static ParameterizedExecutableDescriptor of(Type... paramTypes) {
        return of(List.of(paramTypes));
    }

    static ParameterizedExecutableDescriptor of(List<Type> paramTypes) {
        return new ParameterizedExecutableDescriptor() {
            public List<Type> getParameterTypes() {
                return paramTypes;
            }

            public int getParameterCount() {
                return paramTypes.size();
            }

            public Type getParameterType(final int index) {
                return paramTypes.get(index);
            }

            public Type[] getParameterTypesAsArray() {
                return paramTypes.toArray(Type[]::new);
            }

            public int hashCode() {
                return paramTypes.hashCode();
            }

            public boolean equals(final Object obj) {
                return obj instanceof ParameterizedExecutableDescriptor && equals((ParameterizedExecutableDescriptor) obj);
            }

            boolean equals(final ParameterizedExecutableDescriptor other) {
                return this == other || getParameterTypes().equals(other.getParameterTypes());
            }
        };
    }

    List<Type> getParameterTypes();

    int getParameterCount();

    Type getParameterType(int index);

    Type[] getParameterTypesAsArray();
}
