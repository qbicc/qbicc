package cc.quarkus.qcc.plugin.native_;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
final class NativeFunctionInfo {
    final ExecutableElement origMethod;
    final SymbolLiteral symbolLiteral;

    NativeFunctionInfo(final ExecutableElement origMethod, final SymbolLiteral symbolLiteral) {
        this.origMethod = origMethod;
        this.symbolLiteral = symbolLiteral;
    }
}
