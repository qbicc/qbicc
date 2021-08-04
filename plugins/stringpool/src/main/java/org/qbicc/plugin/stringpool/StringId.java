package org.qbicc.plugin.stringpool;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;

/**
 * Handle to uniquely identify a string in StringPool
 */
public interface StringId {
    Literal serialize(CompilationContext context);
}
