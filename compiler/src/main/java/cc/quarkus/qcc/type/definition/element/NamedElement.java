package org.qbicc.type.definition.element;

import java.util.Objects;

/**
 *
 */
public interface NamedElement extends Element {

    String getName();

    default boolean nameEquals(String name) {
        return Objects.equals(getName(), name);
    }

    interface Builder extends Element.Builder {
        void setName(final String name);

        NamedElement build();
    }
}
