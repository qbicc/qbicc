package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.MethodHandle;

/**
 *
 */
public interface VirtualExecutableElement extends BasicElement {
    boolean hasVirtualMethodBody();

    MethodHandle getVirtualMethodBody();

    interface Builder extends BasicElement.Builder {
        void setVirtualMethodBody(MethodHandle handle);

        VirtualExecutableElement build();
    }
}
