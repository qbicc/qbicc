package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public interface ConstructorDescriptor extends ParameterizedExecutableDescriptor {
    static ConstructorDescriptor of(ParameterizedExecutableDescriptor pd) {
        return new ConstructorDescriptor() {
            public ParameterizedExecutableDescriptor getParameterizedExecutableDescriptor() {
                return pd;
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

            public int hashCode() {
                return 13 * getParameterizedExecutableDescriptor().hashCode();
            }

            public boolean equals(final Object obj) {
                return obj instanceof ConstructorDescriptor && equals((ConstructorDescriptor) obj);
            }

            boolean equals(final ConstructorDescriptor other) {
                return this == other || getParameterizedExecutableDescriptor().equals(other.getParameterizedExecutableDescriptor());
            }
        };
    }

    ParameterizedExecutableDescriptor getParameterizedExecutableDescriptor();
}
