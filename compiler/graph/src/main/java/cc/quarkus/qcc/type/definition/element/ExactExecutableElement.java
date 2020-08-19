package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.MethodHandle;

/**
 *
 */
public interface ExactExecutableElement extends BasicElement {
    boolean hasExactMethodBody();

    MethodHandle getExactMethodBody();

    interface Builder extends BasicElement.Builder {
        void setExactMethodBody(MethodHandle methodHandle);

        ExactExecutableElement build();
    }
}
