package org.qbicc.plugin.opt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.collections.api.factory.Maps;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Call;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.InvokeNoReturn;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Return;
import org.qbicc.graph.Slot;
import org.qbicc.graph.TailCall;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Throw;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ExecutableLiteral;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Declaration;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * The inliner.  Every method call is speculatively inlined unless it is specifically annotated otherwise.
 */
public class InliningBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    // todo: this is arbitrary
    private final float costThreshold = 50.0f;
    private final int depthThreshold = 10;
    private int depth = 0;

    private InliningBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    public static BasicBlockBuilder createIfNeeded(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        if (delegate.getRootElement() instanceof FunctionElement) {
            // do not inline into functions
            return delegate;
        } else {
            return new InliningBasicBlockBuilder(ctxt, delegate);
        }
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(targetPtr);
        if (toInline != null) {
            BlockLabel resumeLabel = new BlockLabel();
            BasicBlock inlined = doInline(receiver, toInline, arguments, null, val -> goto_(resumeLabel, val.getType() instanceof VoidType ? Map.of() : Map.of(Slot.result(), val)), Map.of());
            if (inlined != null) {
                begin(resumeLabel);
                ValueType returnType = toInline.getType().getReturnType();
                return returnType instanceof VoidType ? emptyVoid() : addParam(resumeLabel, Slot.result(), returnType);
            }
        }
        return super.call(targetPtr, receiver, arguments);
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        ExecutableElement toInline = getInlinedElement(targetPtr);
        if (toInline != null) {
            BlockLabel resumeLabel = new BlockLabel();
            BasicBlock inlined = doInline(receiver, toInline, arguments, null, val -> goto_(resumeLabel, val.getType() instanceof VoidType ? Map.of() : Map.of(Slot.result(), val)), Map.of());
            if (inlined != null) {
                begin(resumeLabel);
                ValueType returnType = toInline.getType().getReturnType();
                return returnType instanceof VoidType ? emptyVoid() : addParam(resumeLabel, Slot.result(), returnType);
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
            BasicBlock inlined = doInline(receiver, toInline, arguments, catchLabel, val -> goto_(resumeLabel, val.getType() instanceof VoidType ? targetArguments : addArg(targetArguments, Slot.result(), val)), targetArguments);
            if (inlined != null) {
                ValueType returnType = toInline.getType().getReturnType();
                return returnType instanceof VoidType ? emptyVoid() : addParam(resumeLabel, Slot.result(), returnType);
            }
        }
        return super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    private ExecutableElement getInlinedElement(final Value target) {
        if (target instanceof ExecutableLiteral el) {
            ExecutableElement element = el.getExecutable();
            if (element instanceof MethodElement && element.hasNoModifiersOf(ClassFile.I_ACC_NEVER_INLINE)) {
                return element;
            }
        }
        return null;
    }

    private BasicBlock doInline(Value receiver, ExecutableElement element, List<Value> arguments, BlockLabel catchLabel, Function<Value, BasicBlock> onReturn, Map<Slot, Value> targetArguments) {
        MethodBody body = element.getPreviousMethodBody();
        if (body != null && depth < depthThreshold) {
            // todo: force alwaysInline to true if this is the only possible invocation of the method
            boolean inline = element.hasAllModifiersOf(ClassFile.I_ACC_ALWAYS_INLINE);
            if (! inline) {
                // estimate inlining cost
                inline = estimateInlineCost(body) < costThreshold;
            }
            if (! inline) {
                // no inlining today
                return null;
            }
            // at this point, we are committed to inlining this call
            depth ++;
            try {
                BlockLabel inlinedMethodEntry = new BlockLabel();
                BasicBlock inlinedBlock = goto_(inlinedMethodEntry, buildArguments(receiver, arguments));
                ProgramLocatable oldCallSite = setCallSite(inlinedBlock.getTerminator());
                try {
                    Node.Copier copier = new Node.Copier(body.getEntryBlock(), getFirstBuilder(), ctxt, (ctxt, visitor) ->
                        new Visitor(visitor, onReturn, catchLabel, targetArguments));
                    copier.copyBlockAs(body.getEntryBlock(), inlinedMethodEntry);
                    copier.copyProgram();
                } finally {
                    setCallSite(oldCallSite);
                }
                // inline successful, now copy all declarations known at this point
                copyDeclarations(element);
                // goto the inlined block
                return inlinedBlock;
            } finally {
                depth --;
            }
        } else {
            return null;
        }
    }

    private Map<Slot, Value> buildArguments(final Value receiver, final List<Value> arguments) {
        HashMap<Slot, Value> map = new HashMap<>(arguments.size() + 1);
        if (! (receiver.getType() instanceof VoidType)) {
            map.put(Slot.this_(), receiver);
        }
        for (int i = 0; i < arguments.size(); i ++) {
            map.put(Slot.funcParam(i), arguments.get(i));
        }
        return Map.copyOf(map);
    }

    private int estimateInlineCost(MethodBody body) {
        // for now, just inline based on method size to have *something*
        BasicBlock entryBlock = body.getEntryBlock();
        HashSet<BasicBlock> visited = new HashSet<>();
        return estimateInlineCost(entryBlock, visited, 0);
    }

    private int estimateInlineCost(final BasicBlock block, final HashSet<BasicBlock> visited, int costSoFar) {
        if (visited.add(block)) {
            // todo: exclude block params, gotos, bitcasts, etc.
            costSoFar += block.getInstructions().size();
            if (costSoFar >= costThreshold) {
                // cancel
                return Integer.MAX_VALUE;
            }
            Terminator t = block.getTerminator();
            int cnt = t.getSuccessorCount();
            for (int i = 0; i < cnt; i ++) {
                costSoFar = estimateInlineCost(t.getSuccessor(i), visited, costSoFar);
                if (costSoFar >= costThreshold) {
                    // cancel
                    return Integer.MAX_VALUE;
                }
            }
        }
        return costSoFar;
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

    final class Visitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock> {
        private final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate;
        private final Function<Value, BasicBlock> onReturn;
        private final BlockLabel catchLabel;
        private final Map<Slot, Value> catchArguments;

        /**
         * The visitor which intercepts nested calls and returns.
         *
         * @param delegate the next visitor (not {@code null})
         * @param onReturn the on-return action which receives the return value (not {@code null})
         * @param catchLabel the label to go to on exception, if any (may be {@code null})
         * @param catchArguments the arguments from outside to pass to the {@code catchLabel}, if any (may be empty) (not {@code null})
         */
        Visitor(final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate, final Function<Value, BasicBlock> onReturn, final BlockLabel catchLabel, Map<Slot, Value> catchArguments) {
            this.delegate = delegate;
            this.onReturn = onReturn;
            this.catchLabel = catchLabel;
            this.catchArguments = catchArguments;
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

        // invocations

        @Override
        public Value visit(Node.Copier param, Call node) {
            if (catchLabel != null) {
                // transform to invoke
                param.copyNode(node.getDependency());
                BlockLabel resume = new BlockLabel();
                invoke(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), catchLabel, resume, catchArguments);
                begin(resume);
                ValueType rt = node.getTarget().getReturnType();
                return rt instanceof VoidType ? emptyVoid() : addParam(resume, Slot.result(), rt);
            } else {
                return delegate.visit(param, node);
            }
        }

        @Override
        public Value visit(Node.Copier param, CallNoSideEffects node) {
            if (catchLabel != null) {
                // transform to invoke
                BlockLabel resume = new BlockLabel();
                invoke(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), catchLabel, resume, catchArguments);
                begin(resume);
                ValueType rt = node.getTarget().getReturnType();
                return rt instanceof VoidType ? emptyVoid() : addParam(resume, Slot.result(), rt);
            } else {
                return delegate.visit(param, node);
            }
        }

        @Override
        public BasicBlock visit(Node.Copier param, CallNoReturn node) {
            if (catchLabel != null) {
                // transform to invoke
                param.copyNode(node.getDependency());
                return invokeNoReturn(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), catchLabel, catchArguments);
            } else {
                return delegate.visit(param, node);
            }
        }

        @Override
        public BasicBlock visit(Node.Copier param, TailCall node) {
            if (catchLabel != null) {
                // transform to invoke
                param.copyNode(node.getDependency());
                BlockLabel resume = new BlockLabel();
                invoke(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), catchLabel, resume, catchArguments);
                begin(resume);
                ValueType rt = node.getTarget().getReturnType();
                return onReturn.apply(rt instanceof VoidType ? emptyVoid() : addParam(resume, Slot.result(), rt));
            } else {
                return onReturn.apply(call(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments())));
            }
        }

        @Override
        public BasicBlock visit(Node.Copier copier, Throw node) {
            if (catchLabel != null) {
                // transform to goto
                copier.copyNode(node.getDependency());
                return goto_(catchLabel, addArg(catchArguments, Slot.thrown(), copier.copyValue(node.getThrownValue())));
            } else {
                return delegate.visit(copier, node);
            }
        }

        @Override
        public BasicBlock visit(Node.Copier param, Invoke node) {
            return delegate.visit(param, node);
        }

        @Override
        public BasicBlock visit(Node.Copier param, InvokeNoReturn node) {
            return delegate.visit(param, node);
        }
    }

    private static Map<Slot, Value> addArg(final Map<Slot, Value> targetArguments, final Slot slot, final Value value) {
        return Maps.immutable.ofMap(targetArguments).newWithKeyValue(slot, value).castToMap();
    }
}
