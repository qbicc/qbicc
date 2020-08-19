package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public interface FieldResolver {
    FieldElement resolveField(int index);
}
