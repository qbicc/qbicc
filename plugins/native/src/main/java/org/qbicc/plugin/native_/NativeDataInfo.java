package org.qbicc.plugin.native_;

import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
final class NativeDataInfo {
    final FieldElement fieldElement;
    final boolean defined;
    final ValueType objectType;
    final SymbolLiteral symbolLiteral;

    NativeDataInfo(FieldElement fieldElement, boolean defined, ValueType objectType, SymbolLiteral symbolLiteral) {
        this.fieldElement = fieldElement;
        this.defined = defined;
        this.objectType = objectType;
        this.symbolLiteral = symbolLiteral;
    }
}
