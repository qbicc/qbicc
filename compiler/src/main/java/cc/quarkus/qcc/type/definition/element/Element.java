package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.context.Locatable;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;

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
