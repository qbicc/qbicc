package org.qbicc.plugin.trycatch;

import java.util.HashMap;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Value;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A basic block builder which adds a monitor acquire to the start of the subprogram and adds a monitor release
 * to all possible exit paths.
 */
public class SynchronizedMethodBasicBlockBuilder extends DelegatingBasicBlockBuilder implements BasicBlockBuilder.ExceptionHandlerPolicy {
    private final CompilationContext ctxt;
    private final Value monitor;
    private boolean started;
    private final ReferenceType throwable;
    private ExceptionHandlerPolicy outerPolicy;
    private final Map<ExceptionHandler, ExceptionHandler> handlers = new HashMap<>();

    private SynchronizedMethodBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        ExecutableElement element = getCurrentElement();
        DefinedTypeDefinition enclosing = element.getEnclosingType();
        if (element.isStatic()) {
            monitor = classOf(ctxt.getLiteralFactory().literalOfType(enclosing.load().getType()), ctxt.getLiteralFactory().zeroInitializerLiteralOfType(ctxt.getTypeSystem().getUnsignedInteger8Type()));
        } else {
            monitor = notNull(parameter(enclosing.load().getType().getReference(), "this", 0));
        }
        throwable = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Throwable").load().getClassType().getReference();
    }

    public void setExceptionHandlerPolicy(final ExceptionHandlerPolicy policy) {
        outerPolicy = policy;
    }

    public ExceptionHandler computeCurrentExceptionHandler(final ExceptionHandler delegate) {
        ExceptionHandler handler = handlers.get(delegate);
        if (handler == null) {
            handlers.put(delegate, handler = new ExceptionHandlerImpl(delegate));
        }
        // our handler comes *after* any outer policy's (i.e. class file) handler
        if (outerPolicy != null) {
            handler = outerPolicy.computeCurrentExceptionHandler(handler);
        }
        return handler;
    }

    final class ExceptionHandlerImpl implements ExceptionHandler {
        private final PhiValue phi;
        private final ExceptionHandler delegate;

        ExceptionHandlerImpl(final ExceptionHandler delegate) {
            this.delegate = delegate;
            phi = phi(throwable, new BlockLabel(), PhiValue.Flag.NOT_NULL);
        }

        public BlockLabel getHandler() {
            return phi.getPinnedBlockLabel();
        }

        public void enterHandler(final BasicBlock from, final BasicBlock landingPad, final Value exceptionValue) {
            if (landingPad != null) {
                phi.setValueForBlock(ctxt, getCurrentElement(), landingPad, exceptionValue);
            } else {
                // direct (local) throw
                phi.setValueForBlock(ctxt, getCurrentElement(), from, exceptionValue);
            }
            BlockLabel label = phi.getPinnedBlockLabel();
            if (! label.hasTarget()) {
                // generate the new handler body
                begin(label);
                // release the lock
                monitorExit(monitor);
                // hopefully the delegate simply rethrows
                BasicBlock ourFrom = goto_(delegate.getHandler());
                // direct goto next block (no landing pad)
                delegate.enterHandler(ourFrom, null, phi);
            }
        }
    }

    public Node begin(final BlockLabel blockLabel) {
        Node node = super.begin(blockLabel);
        if (! started) {
            // method start
            started = true;
            Node monitorEnter = monitorEnter(monitor);
            getDelegate().setExceptionHandlerPolicy(this);
            return monitorEnter;
        }
        return node;
    }

    public BasicBlock return_() {
        monitorExit(monitor);
        return super.return_();
    }

    public BasicBlock return_(final Value value) {
        monitorExit(monitor);
        return super.return_(value);
    }

    public BasicBlock throw_(final Value value) {
        // note: this must come *after* the local throw handling builder otherwise problems will result;
        // local throw will transform `throw` to `goto`, resulting in extra monitor releases
        monitorExit(monitor);
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
