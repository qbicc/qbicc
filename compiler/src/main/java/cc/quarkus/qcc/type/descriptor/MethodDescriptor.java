package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public interface MethodDescriptor extends ParameterizedExecutableDescriptor {
    static MethodDescriptor of(ParameterizedExecutableDescriptor pd, ValueType returnType) {
        return new MethodDescriptor() {
            public ValueType getReturnType() {
                return returnType;
            }

            public List<ValueType> getParameterTypes() {
                return pd.getParameterTypes();
            }

            public int getParameterCount() {
                return pd.getParameterCount();
            }

            public ValueType getParameterType(final int index) {
                return pd.getParameterType(index);
            }

            public ValueType[] getParameterTypesAsArray() {
                return pd.getParameterTypesAsArray();
            }

            public ParameterizedExecutableDescriptor getParameterizedExecutableDescriptor() {
                return pd;
            }

            public int hashCode() {
                return returnType.hashCode() * 19 + pd.hashCode();
            }

            public boolean equals(final Object obj) {
                return obj instanceof MethodDescriptor && equals((MethodDescriptor) obj);
            }

            boolean equals(final MethodDescriptor other) {
                return this == other || returnType.equals(other.getReturnType()) && pd.equals(other.getParameterizedExecutableDescriptor());
            }
        };
    }

    ValueType getReturnType();

    ParameterizedExecutableDescriptor getParameterizedExecutableDescriptor();
}
