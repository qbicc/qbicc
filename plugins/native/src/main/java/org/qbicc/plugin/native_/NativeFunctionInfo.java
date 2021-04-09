package org.qbicc.plugin.native_;

import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.type.definition.element.ExecutableElement;

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
