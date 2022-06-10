package org.qbicc.plugin.native_;

import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
final class NativeDataInfo {
    final FieldElement fieldElement;
    final ValueType objectType;
    final PointerLiteral symbolLiteral;

    NativeDataInfo(FieldElement fieldElement, ValueType objectType, PointerLiteral symbolLiteral) {
        this.fieldElement = fieldElement;
        this.objectType = objectType;
        this.symbolLiteral = symbolLiteral;
    }
}
