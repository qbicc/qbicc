package cc.quarkus.qcc.type.descriptor;

import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.graph.Type;

/**
 * A descriptor for a method's type.
 */
public interface MethodTypeDescriptor {
    int getParameterCount();

    Type getParameterType(int index);

    Type getReturnType();

    static MethodTypeDescriptor of(Type returnType, List<Type> paramTypes) {
        return new MethodTypeDescriptor() {
            public int getParameterCount() {
                return paramTypes.size();
            }

            public Type getParameterType(final int index) {
                return paramTypes.get(index);
            }

            public Type getReturnType() {
                return returnType;
            }

            public boolean equals(final Object obj) {
                return obj instanceof MethodTypeDescriptor && equals((MethodTypeDescriptor) obj);
            }

            boolean equals(MethodTypeDescriptor obj) {
                return obj != null && getParameterCount() == obj.getParameterCount()
                    && parameterTypesAreEqual(obj);
            }

            public int hashCode() {
                return Objects.hash(returnType, paramTypes);
            }

            boolean parameterTypesAreEqual(MethodTypeDescriptor other) {
                int cnt = getParameterCount();
                for (int i = 0; i < cnt; i ++) {
                    if (! getParameterType(i).equals(other.getParameterType(i))) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    static MethodTypeDescriptor of(Type returnType, Type... paramTypes) {
        return of(returnType, List.of(paramTypes));
    }
}
