package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.graph.Type;

/**
 *
 */
public interface ConstructorDescriptor extends ParameterizedExecutableDescriptor {
    static ConstructorDescriptor of(ParameterizedExecutableDescriptor pd) {
        return new ConstructorDescriptor() {
            public ParameterizedExecutableDescriptor getParameterizedExecutableDescriptor() {
                return pd;
            }

            public List<Type> getParameterTypes() {
                return pd.getParameterTypes();
            }

            public int getParameterCount() {
                return pd.getParameterCount();
            }

            public Type getParameterType(final int index) {
                return pd.getParameterType(index);
            }

            public Type[] getParameterTypesAsArray() {
                return pd.getParameterTypesAsArray();
            }
        };
    }

    ParameterizedExecutableDescriptor getParameterizedExecutableDescriptor();
}
