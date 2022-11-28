package org.qbicc.plugin.gc.common.safepoint;

import static org.qbicc.type.descriptor.MethodDescriptor.VOID_METHOD_DESCRIPTOR;

import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.type.definition.element.StaticMethodElement;

/**
 * The base class for safepoint strategies which use a method call to poll for a safepoint.
 */
public abstract class AbstractMethodBasedSafePointStrategy extends AbstractSafePointStrategy {

    private static final String POLL_SAFE_POINT = "pollSafePoint";

    /**
     * Construct a new instance.
     * The constructor should inject any additional fields or methods needed to implement the strategy.
     *
     * @param ctxt the compilation context
     */
    protected AbstractMethodBasedSafePointStrategy(CompilationContext ctxt) {
        super(ctxt);
        final Patcher patcher = Patcher.get(ctxt);
        patcher.replaceMethodBody(ctxt.getBootstrapClassContext(), SAFE_POINT_INT_NAME, POLL_SAFE_POINT, VOID_METHOD_DESCRIPTOR, adapt(this::implementPollSafePoint), 0);
    }

    @Override
    protected void forEachSafePointMethod(Consumer<String> consumer) {
        super.forEachSafePointMethod(consumer);
        consumer.accept(POLL_SAFE_POINT);
    }

    @Override
    public void safePoint(BasicBlockBuilder bbb) {
        final ClassContext bcc = bbb.getContext().getBootstrapClassContext();
        final StaticMethodElement pollSafePoint = (StaticMethodElement) bcc.findDefinedType(SAFE_POINT_INT_NAME).load().requireSingleMethod(POLL_SAFE_POINT);
        bbb.call(bbb.getLiteralFactory().literalOf(pollSafePoint), List.of());
    }

    public abstract void implementPollSafePoint(BasicBlockBuilder bbb);

}
