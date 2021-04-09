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

    NestedClassElement(final Builder builder) {
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends BasicElement.Builder implements MemberElement.Builder, NamedElement.Builder {
        private String name;
        private DefinedTypeDefinition correspondingType;

        Builder() {}

        public void setName(final String name) {
            this.name = name;
        }

        public void setCorrespondingType(final DefinedTypeDefinition correspondingType) {
            this.correspondingType = correspondingType;
        }

        public NestedClassElement build() {
            return new NestedClassElement(this);
        }
    }
}
