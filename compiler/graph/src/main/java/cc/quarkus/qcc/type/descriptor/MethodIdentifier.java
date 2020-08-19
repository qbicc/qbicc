package cc.quarkus.qcc.type.descriptor;

import java.util.Objects;

import cc.quarkus.qcc.graph.Type;

/**
 * A unique method identifier.
 */
public interface MethodIdentifier extends MethodTypeDescriptor {

    /**
     * Get the method name.
     *
     * @return the method name (not {@code null})
     */
    String getName();

    static MethodIdentifier of(String name, MethodTypeDescriptor descriptor) {
        return new MethodIdentifier() {
            public String getName() {
                return name;
            }

            public int getParameterCount() {
                return descriptor.getParameterCount();
            }

            public Type getParameterType(final int index) {
                return descriptor.getParameterType(index);
            }

            public Type getReturnType() {
                return descriptor.getReturnType();
            }

            public boolean equals(final Object obj) {
                return obj instanceof MethodIdentifier && equals((MethodIdentifier) obj);
            }

            boolean equals(MethodIdentifier obj) {
                return obj != null && getParameterCount() == obj.getParameterCount() && getName().equals(obj.getName())
                    && parameterTypesAreEqual(obj);
            }

            public int hashCode() {
                return Objects.hash(name, descriptor);
            }

            boolean parameterTypesAreEqual(MethodIdentifier other) {
                int cnt = getParameterCount();
                for (int i = 0; i < cnt; i ++) {
                    if (! getParameterType(i).equals(other.getParameterType(i))) {
                        return false;
                    }
                }
                return true;
            }

            public Type[] getParameterTypesAsArray() {
                return descriptor.getParameterTypesAsArray();
            }
        };
    }
}
