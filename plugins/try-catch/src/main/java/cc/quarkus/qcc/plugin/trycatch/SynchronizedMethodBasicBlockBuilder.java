package cc.quarkus.qcc.plugin.trycatch;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Try;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A basic block builder which adds a monitor acquire to the start of the subprogram and adds a monitor release
 * to all possible exit paths.
 */
public class SynchronizedMethodBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final BlockLabel exitHandlerLabel;
    private final Value monitor;
    private boolean enabled;
    private final ClassTypeIdLiteral throwable;

    private SynchronizedMethodBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        exitHandlerLabel = new BlockLabel();
        ExecutableElement element = getCurrentElement();
        DefinedTypeDefinition enclosing = element.getEnclosingType();
        if (element.isStatic()) {
            // todo: this has to become a class instance
            monitor = enclosing.validate().getTypeId();
        } else {
            monitor = receiver(enclosing.validate().getTypeId());
        }
        throwable = (ClassTypeIdLiteral) ctxt.getBootstrapClassContext().findDefinedType("java/lang/Throwable").validate().getTypeId();
    }

    public Try.CatchMapper setCatchMapper(Try.CatchMapper catchMapper) {
        final Try.CatchMapper original = catchMapper;
        return new Try.CatchMapper.Delegating() {
            public Try.CatchMapper getDelegate() {
                return original;
            }

            public int getCatchCount() {
                return 1 + getDelegate().getCatchCount();
            }

            public ClassTypeIdLiteral getCatchType(final int index) {
                int cnt = getDelegate().getCatchCount();
                if (index < cnt) {
                    return getDelegate().getCatchType(index);
                } else if (index == cnt) {
                    return throwable;
                } else {
                    throw new IndexOutOfBoundsException(index);
                }
            }

            public BlockLabel getCatchHandler(final int index) {
                int cnt = getDelegate().getCatchCount();
                if (index < cnt) {
                    return getDelegate().getCatchHandler(index);
                } else if (index == cnt) {
                    if (! exitHandlerLabel.hasTarget()) {

                    }
                    return exitHandlerLabel;
                } else {
                    throw new IndexOutOfBoundsException(index);
                }
            }

            public void setCatchValue(final int index, final BasicBlock from, final Value value) {

            }
        };
    }

    public Node begin(final BlockLabel blockLabel) {
        Node node = super.begin(blockLabel);
        if (! enabled) {
            // method start
            monitorEnter(monitor);
            // terminate the start block
            BlockLabel realStart = new BlockLabel();
            goto_(realStart);
            // generate the catch handler
            super.begin(exitHandlerLabel);
            Value catchVal = catch_(throwable);
            monitorExit(monitor);
            throw_(catchVal);
            // now start the real start block
            enabled = true;
            node = begin(realStart);
        }
        return node;
    }

    public BasicBlock return_() {
        if (enabled) {
            enabled = false;
            try {
                // todo: this should be triable
                monitorExit(monitor);
                return super.return_();
            } finally {
                enabled = true;
            }
        }
        return super.return_();
    }

    public BasicBlock return_(final Value value) {
        if (enabled) {
            enabled = false;
            try {
                // todo: this should be triable
                monitorExit(monitor);
                return super.return_(value);
            } finally {
                enabled = true;
            }
        }
        return super.return_(value);
    }

    public BasicBlock throw_(final Value value) {
        if (enabled) {
            enabled = false;
            try {
                // todo: this should be triable
                monitorExit(monitor);
                return super.throw_(value);
            } finally {
                enabled = true;
            }
        }
        return super.throw_(value);
    }

    public static BasicBlockBuilder createIfNeeded(CompilationContext ctxt, BasicBlockBuilder delegate) {
        if (delegate.getCurrentElement().hasAllModifiersOf(ClassFile.ACC_SYNCHRONIZED)) {
            return new SynchronizedMethodBasicBlockBuilder(ctxt, delegate);
        } else {
            return delegate;
        }
    }
}
