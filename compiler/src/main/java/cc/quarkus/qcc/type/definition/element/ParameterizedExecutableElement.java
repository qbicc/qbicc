package cc.quarkus.qcc.type.definition.element;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.descriptor.ParameterizedExecutableDescriptor;

/**
 *
 */
public interface ParameterizedExecutableElement extends ExecutableElement {
    int getParameterCount();

    ParameterElement getParameter(int index) throws IndexOutOfBoundsException;

    default <T> void forEachParameter(BiConsumer<T, ParameterElement> consumer, T argument) {
        int cnt = getParameterCount();
        for (int i = 0; i < cnt; i ++) {
            consumer.accept(argument, getParameter(i));
        }
    }

    default void forEachParameter(Consumer<ParameterElement> consumer) {
        int cnt = getParameterCount();
        for (int i = 0; i < cnt; i ++) {
            consumer.accept(getParameter(i));
        }
    }

    default boolean parameterTypesEqual(Type... types) {
        if (types.length != getParameterCount()) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            if (types[i] != getParameter(i).getType()) {
                return false;
            }
        }
        return true;
    }

    default boolean parameterTypesEqual(Collection<Type> types) {
        int cnt = getParameterCount();
        if (types.size() != cnt) {
            return false;
        }
        int i = 0;
        for (Type paramType : types) {
            if (paramType != getParameter(i++).getType()) {
                return false;
            }
        }
        return true;
    }

    ParameterizedExecutableDescriptor getDescriptor();

    interface Builder extends ExecutableElement.Builder {
        void expectParameterCount(int count);

        void addParameter(ParameterElement parameter);

        ParameterizedExecutableElement build();
    }
}
