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

        interface Delegating extends Element.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setName(final String name) {
                getDelegate().setName(name);
            }

            @Override
            default NamedElement build() {
                return getDelegate().build();
            }
        }
    }
}
