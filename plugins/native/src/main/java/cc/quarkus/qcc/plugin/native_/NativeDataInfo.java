package cc.quarkus.qcc.plugin.native_;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.FieldElement;

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
