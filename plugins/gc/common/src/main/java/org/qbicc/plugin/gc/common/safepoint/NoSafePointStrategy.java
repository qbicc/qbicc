package org.qbicc.plugin.gc.common.safepoint;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;

/**
 * A safepoint strategy where no safepoints may be entered.
 */
public final class NoSafePointStrategy extends AbstractSafePointStrategy {

    /**
     * Construct a new instance.
     *
     * @param ctxt the compilation context
     */
    public NoSafePointStrategy(CompilationContext ctxt) {
        super(ctxt);
    }

    @Override
    public void safePoint(BasicBlockBuilder bbb) {
        // no operation
    }

    @Override
    public void implementRequestGlobalSafePoint(BasicBlockBuilder bbb) {
        // no operation
    }

    @Override
    public void implementClearGlobalSafePoint(BasicBlockBuilder bbb) {
        // no operation
    }
}
