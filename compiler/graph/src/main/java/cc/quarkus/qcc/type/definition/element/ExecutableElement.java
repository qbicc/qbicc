package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.MethodHandle;

/**
 *
 */
public interface ExecutableElement extends BasicElement {
    boolean hasMethodBody();

    MethodHandle getMethodBody();

    interface Builder extends BasicElement.Builder {
        void setExactMethodBody(MethodHandle methodHandle);

        ExecutableElement build();
    }
}
