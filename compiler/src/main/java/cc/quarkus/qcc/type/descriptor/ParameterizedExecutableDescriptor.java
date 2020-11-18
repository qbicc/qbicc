package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public interface ParameterizedExecutableDescriptor {
    static ParameterizedExecutableDescriptor of(ValueType... paramTypes) {
        return of(List.of(paramTypes));
    }

    static ParameterizedExecutableDescriptor of(List<ValueType> paramTypes) {
        return new ParameterizedExecutableDescriptor() {
            public List<ValueType> getParameterTypes() {
                return paramTypes;
            }

            public int getParameterCount() {
                return paramTypes.size();
            }

            public ValueType getParameterType(final int index) {
                return paramTypes.get(index);
            }

            public ValueType[] getParameterTypesAsArray() {
                return paramTypes.toArray(ValueType[]::new);
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

    List<ValueType> getParameterTypes();

    int getParameterCount();

    ValueType getParameterType(int index);

    ValueType[] getParameterTypesAsArray();
}
