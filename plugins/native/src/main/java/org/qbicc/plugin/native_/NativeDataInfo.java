package org.qbicc.plugin.native_;

import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
final class NativeDataInfo {
    final FieldElement fieldElement;
    final ValueType objectType;
    final ProgramObjectLiteral symbolLiteral;

    NativeDataInfo(FieldElement fieldElement, ValueType objectType, ProgramObjectLiteral symbolLiteral) {
        this.fieldElement = fieldElement;
        this.objectType = objectType;
        this.symbolLiteral = symbolLiteral;
    }
}
