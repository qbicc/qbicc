package cc.quarkus.qcc.plugin.opt;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.BlockEarlyTermination;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.IsEq;
import cc.quarkus.qcc.graph.IsGe;
import cc.quarkus.qcc.graph.IsGt;
import cc.quarkus.qcc.graph.IsLe;
import cc.quarkus.qcc.graph.IsLt;
import cc.quarkus.qcc.graph.IsNe;
import cc.quarkus.qcc.graph.ConstructorInvocation;
import cc.quarkus.qcc.graph.Convert;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Div;
import cc.quarkus.qcc.graph.Extend;
import cc.quarkus.qcc.graph.FunctionCall;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.InstanceInvocation;
import cc.quarkus.qcc.graph.InstanceInvocationValue;
import cc.quarkus.qcc.graph.Mod;
import cc.quarkus.qcc.graph.Multiply;
import cc.quarkus.qcc.graph.Neg;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.Or;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.Rol;
import cc.quarkus.qcc.graph.Ror;
import cc.quarkus.qcc.graph.Shl;
import cc.quarkus.qcc.graph.Shr;
import cc.quarkus.qcc.graph.StaticInvocation;
import cc.quarkus.qcc.graph.StaticInvocationValue;
import cc.quarkus.qcc.graph.Sub;
import cc.quarkus.qcc.graph.Switch;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Truncate;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.Xor;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * The inliner.  Every method call is speculatively inlined unless it is specifically annotated otherwise.
 */
public class  InliningBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    // todo: this is arbitrary
    private final float costThreshold = 80.0f;
    private float cost;

    public InliningBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        if (! target.hasAllModifiersOf(ClassFile.I_ACC_NEVER_INLINE)) {
            if (target.hasMethodBody()) {
                MethodBody body = target.getPreviousMethodBody();
                if (body != null) {
                    float savedCost = this.cost;
                    boolean alwaysInline = target.hasAllModifiersOf(ClassFile.I_ACC_ALWAYS_INLINE);
                    BlockLabel inlined = new BlockLabel();
                    BlockLabel resume = new BlockLabel();
                    Terminator callSite = goto_(inlined).getTerminator();
                    Node oldCallSite = setCallSite(callSite);
                    try {
                        BasicBlock copied;
                        try {
                            copied = Node.Copier.execute(body.getEntryBlock(), getFirstBuilder(), ctxt, (ctxt, visitor) ->
                                new Visitor(visitor, resume, null, arguments, null, alwaysInline));
                        } catch (BlockEarlyTermination e) {
                            copied = e.getTerminatedBlock();
                        }
                        // inline successful, jump to the inlined code
                        inlined.setTarget(copied);
                        setCallSite(oldCallSite);
                        // this is the return point (it won't be reachable if the inlined function does not return)
                        begin(resume);
                        return nop();
                    } catch (Cancel ignored) {
                        // call site was not inlined; restore original inlining cost
                        this.cost = savedCost;
                        setCallSite(oldCallSite);
                        begin(inlined);
                    }
                }
            }
        }
        return super.invokeStatic(target, arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        if (! target.hasAllModifiersOf(ClassFile.I_ACC_NEVER_INLINE)) {
            if (target.hasMethodBody()) {
                MethodBody body = target.getPreviousMethodBody();
                if (body != null) {
                    float savedCost = this.cost;
                    boolean alwaysInline = target.hasAllModifiersOf(ClassFile.I_ACC_ALWAYS_INLINE);
                    BlockLabel inlined = new BlockLabel();
                    BlockLabel resume = new BlockLabel();
                    Terminator callSite = goto_(inlined).getTerminator();
                    Node oldCallSite = setCallSite(callSite);
                    try {
                        BasicBlock copied;
                        try {
                            copied = Node.Copier.execute(body.getEntryBlock(), getFirstBuilder(), ctxt, (ctxt, visitor) ->
                                new Visitor(visitor, resume, null, arguments, instance, alwaysInline));
                        } catch (BlockEarlyTermination e) {
                            copied = e.getTerminatedBlock();
                        }
                        // inline successful, jump to the inlined code
                        inlined.setTarget(copied);
                        setCallSite(oldCallSite);
                        // this is the return point (it won't be reachable if the inlined function does not return)
                        begin(resume);
                        return nop();
                    } catch (Cancel ignored) {
                        // call site was not inlined; restore original inlining cost
                        this.cost = savedCost;
                        setCallSite(oldCallSite);
                        begin(inlined);
                    }
                }
            }
        }
        return super.invokeInstance(kind, instance, target, arguments);
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        if (! target.hasAllModifiersOf(ClassFile.I_ACC_NEVER_INLINE)) {
            if (target.hasMethodBody()) {
                MethodBody body = target.getPreviousMethodBody();
                if (body != null) {
                    float savedCost = this.cost;
                    boolean alwaysInline = target.hasAllModifiersOf(ClassFile.I_ACC_ALWAYS_INLINE);
                    BlockLabel inlined = new BlockLabel();
                    BlockLabel resume = new BlockLabel();
                    PhiValue returnVal = phi(target.getType().getReturnType(), resume);
                    Terminator callSite = goto_(inlined).getTerminator();
                    Node oldCallSite = setCallSite(callSite);
                    try {
                        BasicBlock copied;
                        try {
                            copied = Node.Copier.execute(body.getEntryBlock(), getFirstBuilder(), ctxt, (ctxt, visitor) ->
                                new Visitor(visitor, resume, returnVal, arguments, null, alwaysInline));
                        } catch (BlockEarlyTermination e) {
                            copied = e.getTerminatedBlock();
                        }
                        // inline successful, jump to the inlined code
                        inlined.setTarget(copied);
                        setCallSite(oldCallSite);
                        // this is the return point (it won't be reachable if the inlined function does not return)
                        begin(resume);
                        return returnVal;
                    } catch (Cancel ignored) {
                        // call site was not inlined; restore original inlining cost
                        this.cost = savedCost;
                        setCallSite(oldCallSite);
                        begin(inlined);
                    }
                }
            }
        }
        return super.invokeValueStatic(target, arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        if (! target.hasAllModifiersOf(ClassFile.I_ACC_NEVER_INLINE)) {
            if (target.hasMethodBody()) {
                MethodBody body = target.getPreviousMethodBody();
                if (body != null) {
                    float savedCost = this.cost;
                    boolean alwaysInline = target.hasAllModifiersOf(ClassFile.I_ACC_ALWAYS_INLINE);
                    BlockLabel inlined = new BlockLabel();
                    BlockLabel resume = new BlockLabel();
                    Terminator callSite = goto_(inlined).getTerminator();
                    Node oldCallSite = setCallSite(callSite);
                    PhiValue returnVal = phi(target.getType().getReturnType(), resume);
                    try {
                        BasicBlock copied;
                        try {
                            copied = Node.Copier.execute(body.getEntryBlock(), getFirstBuilder(), ctxt, (ctxt, visitor) ->
                                new Visitor(visitor, resume, returnVal, arguments, instance, alwaysInline));
                        } catch (BlockEarlyTermination e) {
                            copied = e.getTerminatedBlock();
                        }
                        // inline successful, jump to the inlined code
                        inlined.setTarget(copied);
                        setCallSite(oldCallSite);
                        // this is the return point (it won't be reachable if the inlined function does not return)
                        begin(resume);
                        return returnVal;
                    } catch (Cancel ignored) {
                        // call site was not inlined; restore original inlining cost
                        this.cost = savedCost;
                        setCallSite(oldCallSite);
                        begin(inlined);
                    }
                }
            }
        }
        return super.invokeValueInstance(kind, instance, target, arguments);
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
        private final BlockLabel resume;
        private final PhiValue returnValue;
        private final List<Value> arguments;
        private final Value this_;
        private final boolean alwaysInline;

        Visitor(final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate, final BlockLabel resume, final PhiValue returnValue, final List<Value> arguments, final Value this_, final boolean alwaysInline) {
            this.delegate = delegate;
            this.resume = resume;
            this.returnValue = returnValue;
            this.arguments = arguments;
            this.this_ = this_;
            this.alwaysInline = alwaysInline;
        }

        public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
            return delegate;
        }

        // Value substitutions

        public BasicBlock visit(final Node.Copier param, final Return node) {
            param.copyNode(node.getDependency());
            return param.getBlockBuilder().goto_(resume);
        }

        public BasicBlock visit(final Node.Copier param, final ValueReturn node) {
            try {
                param.copyNode(node.getDependency());
                Value retArg = param.copyValue(node.getReturnValue());
                BasicBlock block = param.getBlockBuilder().goto_(resume);
                ExecutableElement currentElement = param.getBlockBuilder().getCurrentElement();
                returnValue.setValueForBlock(currentElement.getEnclosingType().getContext().getCompilationContext(), currentElement, block, retArg);
                return block;
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

        public Node visit(final Node.Copier param, final InstanceInvocation node) {
            DispatchInvocation.Kind kind = node.getKind();
            // todo: this is totally arbitrary
            addCost(param, kind == DispatchInvocation.Kind.EXACT ? 10 : kind == DispatchInvocation.Kind.VIRTUAL ? 30 : 50);
            return delegate.visit(param, node);
        }

        public Node visit(final Node.Copier param, final StaticInvocation node) {
            addCost(param, 10);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final ConstructorInvocation node) {
            addCost(param, 10);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final InstanceInvocationValue node) {
            DispatchInvocation.Kind kind = node.getKind();
            addCost(param, kind == DispatchInvocation.Kind.EXACT ? 10 : kind == DispatchInvocation.Kind.VIRTUAL ? 30 : 50);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final StaticInvocationValue node) {
            addCost(param, 10);
            return delegate.visit(param, node);
        }

        public Value visit(final Node.Copier param, final FunctionCall node) {
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
}
