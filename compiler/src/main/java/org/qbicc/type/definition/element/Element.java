package org.qbicc.type.definition.element;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.generic.TypeParameterContext;

/**
 *
 */
public interface Element extends Locatable {
    String getSourceFileName();

    int getModifiers();

    int getIndex();

    boolean hasAllModifiersOf(int mask);

    boolean hasNoModifiersOf(int mask);

    <T, R> R accept(ElementVisitor<T, R> visitor, T param);

    DefinedTypeDefinition getEnclosingType();

    default TypeParameterContext getTypeParameterContext() {
        return getEnclosingType();
    }

    @Override
    default Location getLocation() {
        return Location.builder().setElement(this).build();
    }

    interface Builder {
        void setModifiers(int modifiers);

        void setIndex(int index);

        void setEnclosingType(DefinedTypeDefinition enclosingType);
    }
}
