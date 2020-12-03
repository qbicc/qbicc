package cc.quarkus.qcc.plugin.native_;

import cc.quarkus.qcc.graph.literal.SymbolLiteral;

/**
 *
 */
final class NativeFunctionInfo {
    final SymbolLiteral symbolLiteral;

    NativeFunctionInfo(final SymbolLiteral symbolLiteral) {
        this.symbolLiteral = symbolLiteral;
    }
}
