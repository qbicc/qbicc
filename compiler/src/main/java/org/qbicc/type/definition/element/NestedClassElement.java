package org.qbicc.type.definition.element;

import org.qbicc.type.definition.DefinedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 * A class element, representing a class that is enclosed within another class.
 */
public final class NestedClassElement extends BasicElement implements MemberElement, NamedElement {
    public static final NestedClassElement[] NO_NESTED_CLASSES = new NestedClassElement[0];

    private final String name;
    private final DefinedTypeDefinition correspondingType;

    NestedClassElement(final BuilderImpl builder) {
        super(builder);
        this.name = Assert.checkNotNullParam("builder.name", builder.name);
        this.correspondingType = Assert.checkNotNullParam("builder.correspondingType", builder.correspondingType);
    }

    public String getName() {
        return name;
    }

    public DefinedTypeDefinition getCorrespondingType() {
        return correspondingType;
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder(int index) {
        return new BuilderImpl(index);
    }

    public interface Builder extends BasicElement.Builder, MemberElement.Builder, NamedElement.Builder {
        void setName(final String name);

        void setCorrespondingType(final DefinedTypeDefinition correspondingType);

        NestedClassElement build();

        interface Delegating extends BasicElement.Builder.Delegating, MemberElement.Builder.Delegating, NamedElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setCorrespondingType(final DefinedTypeDefinition correspondingType) {
                getDelegate().setCorrespondingType(correspondingType);
            }

            @Override
            default NestedClassElement build() {
                return getDelegate().build();
            }
        }
    }

    static final class BuilderImpl extends BasicElement.BuilderImpl implements Builder {
        private String name;
        private DefinedTypeDefinition correspondingType;

        BuilderImpl(int index) {
            super(index);
        }

        public void setCorrespondingType(final DefinedTypeDefinition correspondingType) {
            this.correspondingType = correspondingType;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        public NestedClassElement build() {
            return new NestedClassElement(this);
        }
    }
}
