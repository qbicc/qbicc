package org.qbicc.plugin.opt;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.collections.api.factory.Maps;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Add;
import org.qbicc.graph.And;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.Call;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Div;
import org.qbicc.graph.EncodeReference;
import org.qbicc.graph.Extend;
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
import org.qbicc.graph.Return;
import org.qbicc.graph.Rol;
import org.qbicc.graph.Ror;
import org.qbicc.graph.Shl;
import org.qbicc.graph.Shr;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Sub;
import org.qbicc.graph.Switch;
import org.qbicc.graph.TailCall;
import org.qbicc.graph.Throw;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.Xor;
import org.qbicc.graph.literal.ExecutableLiteral;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Declaration;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * The inliner.  Every method call is speculatively inlined unless it is specifically annotated otherwise.
 */
public class InliningBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    // todo: this is arbitrary
    private final float costThreshold = 80.0f;
    private float cost;

    public InliningBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(targetPtr);
        if (toInline != null) {
            BlockLabel resumeLabel = new BlockLabel();
            BasicBlock inlined = doInline(receiver, toInline, arguments, null, val -> goto_(resumeLabel, Slot.result(), val), Map.of());
            if (inlined != null) {
                begin(resumeLabel);
                return addParam(resumeLabel, Slot.result(), toInline.getType().getReturnType());
            }
        }
        return super.call(targetPtr, receiver, arguments);
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(targetPtr);
        if (toInline != null) {
            BlockLabel resumeLabel = new BlockLabel();
            BasicBlock inlined = doInline(receiver, toInline, arguments, null, val -> goto_(resumeLabel, Map.of(Slot.result(), val)), Map.of());
            if (inlined != null) {
                begin(resumeLabel);
                return addParam(resumeLabel, Slot.result(), toInline.getType().getReturnType());
            }
        }
        return super.callNoSideEffects(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(targetPtr);
        if (toInline != null) {
            BasicBlock inlined = doInline(receiver, toInline, arguments, null, val -> {
                ctxt.error(getLocation(), "Invalid return from noreturn method");
                throw new BlockEarlyTermination(unreachable());
            }, Map.of());
            if (inlined != null) {
                return inlined;
            }
        }
        return super.callNoReturn(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        ExecutableElement toInline = getInlinedElement(targetPtr);
        if (toInline != null) {
            BasicBlock inlined = doInline(receiver, toInline, arguments, catchLabel, val -> {
                ctxt.error(getLocation(), "Invalid return from noreturn method");
                throw new BlockEarlyTermination(unreachable());
            }, targetArguments);
            if (inlined != null) {
                return inlined;
            }
        }
        return super.invokeNoReturn(targetPtr, receiver, arguments, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(targetPtr);
        if (toInline != null) {
            BasicBlock inlined = doInline(receiver, toInline, arguments, null, this::return_, Map.of());
            if (inlined != null) {
                return inlined;
            }
        }
        return super.tailCall(targetPtr, receiver, arguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        ExecutableElement toInline = getInlinedElement(targetPtr);
        if (toInline != null) {
            BasicBlock inlined = doInline(receiver, toInline, arguments, catchLabel, val -> goto_(resumeLabel, addArg(targetArguments, Slot.result(), val)), targetArguments);
            if (inlined != null) {
                return addParam(resumeLabel, Slot.result(), toInline.getType().getReturnType());
            }
        }
        return super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    private ExecutableElement getInlinedElement(final Value target) {
        if (target instanceof ExecutableLiteral el) {
            ExecutableElement element = el.getExecutable();
            if (element != null && element.hasNoModifiersOf(ClassFile.I_ACC_NEVER_INLINE)) {
                return element;
            }
        }
        return null;
    }

    private BasicBlock doInline(Value receiver, ExecutableElement element, List<Value> arguments, BlockLabel catchLabel, Function<Value, BasicBlock> onReturn, Map<Slot, Value> targetArguments) {
        MethodBody body = element.getPreviousMethodBody();
        if (body != null) {
            float savedCost = this.cost;
            // todo: force alwaysInline to true if this is the only possible invocation of the method
            boolean alwaysInline = element.hasAllModifiersOf(ClassFile.I_ACC_ALWAYS_INLINE);
            BlockLabel inlined = new BlockLabel();
            try {
                begin(inlined, bbb -> {
                    try {
                        BlockEntry blockEntry = bbb.getBlockEntry();
                        setCallSite(blockEntry);
                        BasicBlock origEntryBlock = body.getEntryBlock();
                        Node.Copier copier = new Node.Copier(origEntryBlock, getFirstBuilder(), ctxt, (ctxt, visitor) ->
                            new Visitor(visitor, arguments, receiver, onReturn, catchLabel, inlined, alwaysInline, targetArguments));
                        copier.copyBlockAs(origEntryBlock, inlined);
                        copier.copyScheduledNodes(origEntryBlock);
                    } catch (BlockEarlyTermination ignored) {
                    }
                    // inline successful, now copy all declarations known at this point
                    copyDeclarations(element);
                });
            } catch (Cancel ignored) {
                // call site was not inlined; restore original inlining cost
                this.cost = savedCost;
                return null;
            }
            // goto the inlined block
            return goto_(inlined, Map.of());
        } else {
            return null;
        }
    }

    private void copyDeclarations(final ExecutableElement target) {
        ProgramModule ourModule = ctxt.getOrAddProgramModule(getRootElement().getEnclosingType());
        ProgramModule module = ctxt.getOrAddProgramModule(target.getEnclosingType());
        for (Declaration decl : module.declarations()) {
            if (decl instanceof FunctionDeclaration declaration) {
                ourModule.declareFunction(declaration);
            } else if (decl instanceof DataDeclaration declaration) {
                ourModule.declareData(declaration);
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

    final class Visitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock> {
        private final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate;
        private final List<Value> arguments;
        private final Value this_;
        private final Function<Value, BasicBlock> onReturn;
        private final BlockLabel catchLabel;
        private final BlockLabel entryBlock;
        private final boolean alwaysInline;
        private final Map<Slot, Value> targetArguments;

        Visitor(final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate, final List<Value> arguments, final Value this_, final Function<Value, BasicBlock> onReturn, final BlockLabel catchLabel, BlockLabel entryBlock, final boolean alwaysInline, Map<Slot, Value> targetArguments) {
            this.delegate = delegate;
            this.arguments = arguments;
            this.this_ = this_;
            this.onReturn = onReturn;
            this.catchLabel = catchLabel;
            this.entryBlock = entryBlock;
            this.alwaysInline = alwaysInline;
            this.targetArguments = targetArguments;
        }

        public NodeVisitor<Node.Copier, Value, Node, BasicBlock> getDelegateNodeVisitor() {
            return delegate;
        }

        // Value substitutions

        public BasicBlock visit(final Node.Copier param, final Return node) {
            try {
                param.copyNode(node.getDependency());
                return onReturn.apply(param.copyValue(node.getReturnValue()));
            } catch (BlockEarlyTermination e) {
                return e.getTerminatedBlock();
            }
        }

        @Override
        public Value visit(final Node.Copier copier, final BlockParameter node) {
            if (node.getPinnedBlockLabel().equals(entryBlock)) {
                Slot slot = node.getSlot();
                if (slot == Slot.this_()) {
                    return this_;
                } else if (slot.getName().equals("p")) {
                    return arguments.get(slot.getIndex());
                }
            }
            return delegate.visit(copier, node);
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

        public Value visit(final Node.Copier param, final DecodeReference node) {
            addCost(param, 1);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final EncodeReference node) {
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
                Value result = invoke(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), catchLabel, resume, targetArguments);
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
                Value result = invoke(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), catchLabel, resume, targetArguments);
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
                return invokeNoReturn(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), catchLabel, targetArguments);
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
                BlockLabel resume = new BlockLabel();
                Value result = invoke(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), catchLabel, resume, targetArguments);
                begin(resume);
                return return_(result);
            } else {
                return delegate.visit(param, node);
            }
        }

        @Override
        public BasicBlock visit(Node.Copier copier, Throw node) {
            if (catchLabel != null) {
                // transform to goto
                copier.copyNode(node.getDependency());
                return goto_(catchLabel, addArg(targetArguments, Slot.thrown(), copier.copyValue(node.getThrownValue())));
            } else {
                return delegate.visit(copier, node);
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

    private static Map<Slot, Value> addArg(final Map<Slot, Value> targetArguments, final Slot slot, final Value value) {
        return Maps.immutable.ofMap(targetArguments).newWithKeyValue(slot, value).castToMap();
    }
}
