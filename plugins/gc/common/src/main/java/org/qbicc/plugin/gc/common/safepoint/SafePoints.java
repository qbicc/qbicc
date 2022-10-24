package org.qbicc.plugin.gc.common.safepoint;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;

/**
 *
 */
public final class SafePoints {
    private static final AttachmentKey<AbstractSafePointStrategy> IMPL_KEY = new AttachmentKey<>();

    private SafePoints() {}

    public static void selectStrategy(CompilationContext ctxt, Strategy strategy) {
        final AbstractSafePointStrategy existing = ctxt.putAttachmentIfAbsent(IMPL_KEY, strategy.create(ctxt));
        if (existing != null) {
            throw new IllegalStateException("Already installed");
        }
    }

    public static void enqueueMethods(CompilationContext ctxt) {
        ctxt.getAttachment(IMPL_KEY).registerReachableMethods(ctxt);
    }

    /**
     * Create the basic block builder for the selected strategy.
     *
     * @param ctxt the compilation context (must not be {@code null})
     * @param delegate the delegate basic block builder (must not be {@code null})
     * @return the basic block builder (not {@code null})
     */
    public static BasicBlockBuilder createBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        final AbstractSafePointStrategy strategy = ctxt.getAttachment(IMPL_KEY);
        return new DelegatingBasicBlockBuilder(delegate) {
            @Override
            public Node safePoint() {
                strategy.safePoint(getFirstBuilder());
                return nop();
            }
        };
    }

    public enum Strategy {
        NONE {
            @Override
            AbstractSafePointStrategy create(CompilationContext ctxt) {
                return new NoSafePointStrategy(ctxt);
            }
        },
        GLOBAL_FLAG {
            @Override
            AbstractSafePointStrategy create(CompilationContext ctxt) {
                return new GlobalFlagSafePointStrategy(ctxt);
            }
        },
        ;

        abstract AbstractSafePointStrategy create(CompilationContext ctxt);
    }
}
