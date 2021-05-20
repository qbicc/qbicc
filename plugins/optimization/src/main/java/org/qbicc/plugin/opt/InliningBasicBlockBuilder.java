package org.qbicc.plugin.opt;

import java.util.List;
import java.util.function.Function;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Add;
import org.qbicc.graph.And;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Call;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.Convert;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Div;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.Extend;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.If;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.InvokeNoReturn;
import org.qbicc.graph.IsEq;
import org.qbicc.graph.IsGe;
import org.qbicc.graph.IsGt;
import org.qbicc.graph.IsLe;
import org.qbicc.graph.IsLt;
import org.qbicc.graph.IsNe;
import org.qbicc.graph.Mod;
import org.qbicc.graph.Multiply;
import org.qbicc.graph.Neg;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Or;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Return;
import org.qbicc.graph.Rol;
import org.qbicc.graph.Ror;
import org.qbicc.graph.Shl;
import org.qbicc.graph.Shr;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Sub;
import org.qbicc.graph.Switch;
import org.qbicc.graph.TailCall;
import org.qbicc.graph.TailInvoke;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.ValueReturn;
import org.qbicc.graph.Xor;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ProgramObject;
import org.qbicc.object.Section;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * The inliner.  Every method call is speculatively inlined unless it is specifically annotated otherwise.
 */
public class  InliningBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, ExecutableElement> {
    private final CompilationContext ctxt;
    private final ExecutableElement rootElement;
    // todo: this is arbitrary
    private final float costThreshold = 80.0f;
    private float cost;

    public InliningBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        rootElement = getCurrentElement();
        this.ctxt = ctxt;
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(target);
        if (toInline != null) {
            BlockLabel resumeLabel = new BlockLabel();
            PhiValue returnVal = phi(toInline.getType().getReturnType(), resumeLabel);
            BasicBlock inlined = doInline(target, toInline, arguments, null, val -> {
                BasicBlock basicBlock = goto_(resumeLabel);
                returnVal.setValueForBlock(ctxt, toInline, basicBlock, val);
                return basicBlock;
            }, () -> begin(resumeLabel));
            if (inlined != null) {
                return returnVal;
            }
        }
        return super.call(target, arguments);
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(target);
        if (toInline != null) {
            BlockLabel resumeLabel = new BlockLabel();
            PhiValue returnVal = phi(toInline.getType().getReturnType(), resumeLabel);
            BasicBlock inlined = doInline(target, toInline, arguments, null, val -> {
                BasicBlock basicBlock = goto_(resumeLabel);
                returnVal.setValueForBlock(ctxt, toInline, basicBlock, val);
                return basicBlock;
            }, () -> begin(resumeLabel));
            if (inlined != null) {
                return returnVal;
            }
        }
        return super.callNoSideEffects(target, arguments);
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(target);
        if (toInline != null) {
            BasicBlock inlined = doInline(target, toInline, arguments, null, val -> {
                ctxt.error(getLocation(), "Invalid return from noreturn method");
                throw new BlockEarlyTermination(unreachable());
            }, () -> {});
            if (inlined != null) {
                return inlined;
            }
        }
        return super.callNoReturn(target, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        ExecutableElement toInline = getInlinedElement(target);
        if (toInline != null) {
            BasicBlock inlined = doInline(target, toInline, arguments, catchLabel, val -> {
                ctxt.error(getLocation(), "Invalid return from noreturn method");
                throw new BlockEarlyTermination(unreachable());
            }, () -> {});
            if (inlined != null) {
                return inlined;
            }
        }
        return super.invokeNoReturn(target, arguments, catchLabel);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(target);
        if (toInline != null) {
            BasicBlock inlined = doInline(target, toInline, arguments, null, this::return_, () -> {});
            if (inlined != null) {
                return inlined;
            }
        }
        return super.tailCall(target, arguments);
    }

    @Override
    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        ExecutableElement toInline = getInlinedElement(target);
        if (toInline != null) {
            BasicBlock inlined = doInline(target, toInline, arguments, catchLabel, this::return_, () -> {});
            if (inlined != null) {
                return inlined;
            }
        }
        return super.tailInvoke(target, arguments, catchLabel);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        ExecutableElement toInline = getInlinedElement(target);
        if (toInline != null) {
            PhiValue returnVal = phi(toInline.getType().getReturnType(), resumeLabel);
            BasicBlock inlined = doInline(target, toInline, arguments, catchLabel, val -> {
                BasicBlock basicBlock = goto_(resumeLabel);
                returnVal.setValueForBlock(ctxt, toInline, basicBlock, val);
                return basicBlock;
            }, () -> {});
            if (inlined != null) {
                return returnVal;
            }
        }
        return super.invoke(target, arguments, catchLabel, resumeLabel);
    }

    private ExecutableElement getInlinedElement(final ValueHandle target) {
        ExecutableElement element = target.accept(this, null);
        if (element != null && element.hasNoModifiersOf(ClassFile.I_ACC_NEVER_INLINE)) {
            return element;
        } else {
            return null;
        }
    }

    // all the value handles we could inline

    @Override
    public ExecutableElement visitUnknown(Void param, ValueHandle node) {
        return null;
    }

    @Override
    public ExecutableElement visit(Void param, FunctionElementHandle node) {
        // only inline functions from functions
        return getCurrentElement() instanceof FunctionElement ? node.getExecutable() : null;
    }

    @Override
    public ExecutableElement visit(Void param, ConstructorElementHandle node) {
        return getCurrentElement() instanceof FunctionElement ? null : node.getExecutable();
    }

    @Override
    public ExecutableElement visit(Void param, ExactMethodElementHandle node) {
        return getCurrentElement() instanceof FunctionElement ? null : node.getExecutable();
    }

    @Override
    public ExecutableElement visit(Void param, StaticMethodElementHandle node) {
        return getCurrentElement() instanceof FunctionElement ? null : node.getExecutable();
    }

    private BasicBlock doInline(ValueHandle target, ExecutableElement element, List<Value> arguments, BlockLabel catchLabel, Function<Value, BasicBlock> onReturn, Runnable andThen) {
        MethodBody body = element.getPreviousMethodBody();
        if (body != null) {
            float savedCost = this.cost;
            boolean alwaysInline = element.hasAllModifiersOf(ClassFile.I_ACC_ALWAYS_INLINE);
            BlockLabel inlined = new BlockLabel();
            BasicBlock fromBlock = goto_(inlined);
            Terminator callSite = fromBlock.getTerminator();
            Node oldCallSite = setCallSite(callSite);
            try {
                BasicBlock copied;
                try {
                    copied = Node.Copier.execute(body.getEntryBlock(), getFirstBuilder(), ctxt, (ctxt, visitor) ->
                        new Visitor(visitor, arguments, target, onReturn, catchLabel, alwaysInline));
                } catch (BlockEarlyTermination e) {
                    copied = e.getTerminatedBlock();
                }
                // inline successful, now copy all declarations known at this point
                copyDeclarations(element);
                // jump to the inlined code
                inlined.setTarget(copied);
                setCallSite(oldCallSite);
                // this is the return point (it won't be reachable if the inlined function does not return)
                andThen.run();
                return fromBlock;
            } catch (Cancel ignored) {
                // call site was not inlined; restore original inlining cost
                this.cost = savedCost;
                setCallSite(oldCallSite);
                begin(inlined);
                return null;
            }
        } else {
            return null;
        }
    }

    private void copyDeclarations(final ExecutableElement target) {
        ProgramModule ourModule = ctxt.getOrAddProgramModule(rootElement.getEnclosingType());
        ProgramModule module = ctxt.getOrAddProgramModule(target.getEnclosingType());
        for (Section section : module.sections()) {
            for (ProgramObject object : section.contents()) {
                if (object instanceof FunctionDeclaration) {
                    FunctionDeclaration declaration = (FunctionDeclaration) object;
                    ourModule.getOrAddSection(section.getName()).declareFunction(
                        declaration.getOriginalElement(),
                        declaration.getName(),
                        declaration.getType()
                    );
                } else if (object instanceof DataDeclaration) {
                    DataDeclaration declaration = (DataDeclaration) object;
                    ourModule.getOrAddSection(section.getName()).declareData(
                        declaration.getOriginalElement(),
                        declaration.getName(),
                        declaration.getType()
                    );
                }
            }
        }
    }

    /**
     * This is thrown to escape the inliner if the speculation failed.
     */
    @SuppressWarnings("serial")
    static final class Cancel extends RuntimeException {
        Cancel() {
            super(null, null, false, false);
        }
    }

    final class Visitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
        private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;
        private final List<Value> arguments;
        private final Value this_;
        private final Function<Value, BasicBlock> onReturn;
        private final BlockLabel catchLabel;
        private final boolean alwaysInline;

        Visitor(final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate, final BlockLabel resume, final PhiValue returnValue, final List<Value> arguments, final Value this_, final boolean alwaysInline) {
            this(delegate, arguments, this_, val -> {
                BasicBlock basicBlock = goto_(resume);
                returnValue.setValueForBlock(ctxt, getCurrentElement(), basicBlock, val);
                return basicBlock;
            }, null, alwaysInline);
        }

        Visitor(final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate, final List<Value> arguments, final ValueHandle target, final Function<Value, BasicBlock> onReturn, final BlockLabel catchLabel, final boolean alwaysInline) {
            this(delegate, arguments, target.hasValueHandleDependency() ? referenceTo(target.getValueHandle()) : null, onReturn, catchLabel, alwaysInline);
        }

        Visitor(final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate, final List<Value> arguments, final Value this_, final Function<Value, BasicBlock> onReturn, final BlockLabel catchLabel, final boolean alwaysInline) {
            this.delegate = delegate;
            this.arguments = arguments;
            this.this_ = this_;
            this.onReturn = onReturn;
            this.catchLabel = catchLabel;
            this.alwaysInline = alwaysInline;
        }

        public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
            return delegate;
        }

        // Value substitutions

        public BasicBlock visit(final Node.Copier param, final Return node) {
            param.copyNode(node.getDependency());
            return onReturn.apply(null);
        }

        public BasicBlock visit(final Node.Copier param, final ValueReturn node) {
            try {
                param.copyNode(node.getDependency());
                return onReturn.apply(param.copyValue(node.getReturnValue()));
            } catch (BlockEarlyTermination e) {
                return e.getTerminatedBlock();
            }
        }

        public Value visit(final Node.Copier param, final ParameterValue node) {
            if (node.getLabel().equals("this")) {
                return this_;
            } else {
                return arguments.get(node.getIndex());
            }
        }

        // Operations with a cost

        // Simple ops

        public Value visit(final Node.Copier param, final Add node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final And node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Div node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Mod node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Multiply node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Neg node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Or node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Sub node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Xor node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final IsEq node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final IsGe node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final IsGt node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final IsLe node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final IsLt node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final IsNe node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final BitCast node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Convert node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Extend node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Truncate node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Rol node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Ror node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Shl node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final Shr node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        // terminators

        public BasicBlock visit(final Node.Copier param, final If node) {
            addCost(param, 4);
            return delegate.visit(param, node);
        }

        public BasicBlock visit(final Node.Copier param, final Switch node) {
            addCost(param, 2 * (node.getNumberOfValues() + 1));
            return delegate.visit(param, node);
        }

        // invocations

        @Override
        public Value visit(Node.Copier param, Call node) {
            // todo: this is totally arbitrary
            addCost(param, 10);
            if (catchLabel != null) {
                // transform to invoke
                param.copyNode(node.getDependency());
                BlockLabel resume = new BlockLabel();
                Value result = invoke(param.copyValueHandle(node.getValueHandle()), param.copyValues(arguments), catchLabel, resume);
                begin(resume);
                return result;
            } else {
                return delegate.visit(param, node);
            }
        }

        @Override
        public Value visit(Node.Copier param, CallNoSideEffects node) {
            // todo: this is totally arbitrary
            addCost(param, 10);
            if (catchLabel != null) {
                // transform to invoke
                BlockLabel resume = new BlockLabel();
                Value result = invoke(param.copyValueHandle(node.getValueHandle()), param.copyValues(arguments), catchLabel, resume);
                begin(resume);
                return result;
            } else {
                return delegate.visit(param, node);
            }
        }

        @Override
        public BasicBlock visit(Node.Copier param, CallNoReturn node) {
            // todo: this is totally arbitrary
            addCost(param, 10);
            if (catchLabel != null) {
                // transform to invoke
                param.copyNode(node.getDependency());
                return invokeNoReturn(param.copyValueHandle(node.getValueHandle()), param.copyValues(arguments), catchLabel);
            } else {
                return delegate.visit(param, node);
            }
        }

        @Override
        public BasicBlock visit(Node.Copier param, TailCall node) {
            // todo: this is totally arbitrary
            addCost(param, 10);
            if (catchLabel != null) {
                // transform to invoke
                param.copyNode(node.getDependency());
                return tailInvoke(param.copyValueHandle(node.getValueHandle()), param.copyValues(arguments), catchLabel);
            } else {
                return delegate.visit(param, node);
            }
        }

        @Override
        public BasicBlock visit(Node.Copier param, Invoke node) {
            addCost(param, 10);
            return delegate.visit(param, node);
        }

        @Override
        public BasicBlock visit(Node.Copier param, InvokeNoReturn node) {
            addCost(param, 10);
            return delegate.visit(param, node);
        }

        @Override
        public BasicBlock visit(Node.Copier param, TailInvoke node) {
            addCost(param, 10);
            return delegate.visit(param, node);
        }

        // invocations - old

        void addCost(final Node.Copier copier, int amount) {
            if (! alwaysInline) {
                float cost = InliningBasicBlockBuilder.this.cost + amount;
                if (cost >= costThreshold) {
                    // force termination
                    try {
                        copier.getBlockBuilder().unreachable();
                    } catch (IllegalStateException | BlockEarlyTermination ignored) {}
                    throw new Cancel();
                }
                InliningBasicBlockBuilder.this.cost = cost;
            }
        }
    }
}
