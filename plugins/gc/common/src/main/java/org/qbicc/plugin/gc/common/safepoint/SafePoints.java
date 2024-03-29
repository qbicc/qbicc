package org.qbicc.plugin.gc.common.safepoint;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public final class SafePoints {
    private static final String THREAD_NATIVE_INT_NAME = "jdk/internal/thread/ThreadNative";

    private SafePoints() {}

    /**
     * Create the basic block builder for the selected strategy.
     *
     * @param fc the factory context (must not be {@code null})
     * @param delegate the delegate basic block builder (must not be {@code null})
     * @return the basic block builder (not {@code null})
     */
    public static BasicBlockBuilder createBasicBlockBuilder(BasicBlockBuilder.FactoryContext fc, BasicBlockBuilder delegate) {
        final ClassContext bcc = delegate.getContext().getBootstrapClassContext();
        final DefinedTypeDefinition dt = bcc.findDefinedType(THREAD_NATIVE_INT_NAME);
        return new DelegatingBasicBlockBuilder(delegate) {

            @Override
            public Node pollSafePoint() {
                final MethodElement pollSafePoint = dt.load().requireSingleMethod("pollSafePoint");
                return getFirstBuilder().call(getLiteralFactory().literalOf(pollSafePoint), List.of());
            }

            @Override
            public Node enterSafePoint(Value setBits, Value clearBits) {
                final MethodElement enterSafePoint = dt.load().requireSingleMethod("enterSafePoint");
                return getFirstBuilder().call(getLiteralFactory().literalOf(enterSafePoint), List.of(setBits, clearBits));
            }

            @Override
            public Node exitSafePoint(Value setBits, Value clearBits) {
                final MethodElement exitSafePoint = dt.load().requireSingleMethod("exitSafePoint");
                return getFirstBuilder().call(getLiteralFactory().literalOf(exitSafePoint), List.of(setBits, clearBits));
            }
        };
    }
}
