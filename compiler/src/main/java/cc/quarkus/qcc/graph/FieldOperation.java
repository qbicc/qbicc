package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 * An operation on a field.
 */
public interface FieldOperation extends Node {
    FieldElement getFieldElement();

    JavaAccessMode getMode();
}
