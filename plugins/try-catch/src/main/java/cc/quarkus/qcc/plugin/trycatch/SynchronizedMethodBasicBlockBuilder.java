package cc.quarkus.qcc.plugin.trycatch;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

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
            monitor = classOf(ctxt.getLiteralFactory().literalOfType(enclosing.validate().getType()));
        } else {
            monitor = parameter(enclosing.validate().getType(), "this", 0);
        }
        throwable = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Throwable").validate().getClassType().getReference();
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
            phi = phi(throwable, new BlockLabel());
        }

        public BlockLabel getHandler() {
            return phi.getPinnedBlockLabel();
        }

        public void enterHandler(final BasicBlock from, final Value exceptionValue) {
            phi.setValueForBlock(ctxt, getCurrentElement(), from, exceptionValue);
            BlockLabel label = phi.getPinnedBlockLabel();
            if (! label.hasTarget()) {
                // generate the new handler body
                begin(label);
                // release the lock
                monitorEnter(monitor);
                // hopefully the delegate simply rethrows
                BasicBlock ourFrom = goto_(delegate.getHandler());
                delegate.enterHandler(ourFrom, phi);
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
