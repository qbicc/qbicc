package org.qbicc.plugin.opt.ea;

import static java.lang.Boolean.TRUE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Action;
import org.qbicc.graph.Add;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.Call;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.Executable;
import org.qbicc.graph.Extend;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.IsEq;
import org.qbicc.graph.IsGe;
import org.qbicc.graph.IsGt;
import org.qbicc.graph.IsLe;
import org.qbicc.graph.IsLt;
import org.qbicc.graph.IsNe;
import org.qbicc.graph.Load;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.Select;
import org.qbicc.graph.Slot;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Store;
import org.qbicc.graph.Sub;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Throw;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;
import org.qbicc.graph.Return;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.NumericType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ElementVisitor;
import org.qbicc.type.definition.element.ExecutableElement;

public class EscapeAnalysisIntraMethodAnalysis implements ElementVisitor<CompilationContext, Void> {

    public Void visitUnknown(CompilationContext param, BasicElement basicElement) {
        if (basicElement instanceof ExecutableElement element) {
            if (element.hasMethodBody()) {
                MethodBody methodBody = element.getMethodBody();
                process(element, methodBody, param);
            }
        }
        return null;
    }

    private void process(ExecutableElement element, MethodBody methodBody, CompilationContext ctxt) {
        final ConnectionGraph connectionGraph = new ConnectionGraph(element);

        final EscapeAnalysisState escapeAnalysisState = EscapeAnalysisState.get(ctxt);
        escapeAnalysisState.addMethod(element, connectionGraph);

        final AnalysisContext analysisContext = new AnalysisContext(escapeAnalysisState, connectionGraph, ctxt.getBootstrapClassContext());
        analysisContext.process(element, methodBody.getEntryBlock());
    }

    static final class AnalysisVisitor implements NodeVisitor<AnalysisContext, Void, Void, Void, Void> {
        private final ExecutableElement element;

        public AnalysisVisitor(ExecutableElement element) {
            this.element = element;
        }

        @Override
        public Void visit(AnalysisContext param, New node) {
            if (visitKnown(param, node)) {
                param.connectionGraph.setNewEscapeValue(node, defaultEscapeValue(param, node.getClassObjectType()));
            }

            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Store node) {
            if (visitKnown(param, node)) {
                final PointerValue handle = node.getPointerValue();
                final Value value = node.getValue();

                if (handle instanceof InstanceFieldOf fieldOf && fieldOf.getPointerValue() instanceof ReferenceHandle ref) {
                    if (value instanceof New) {
                        if (isThisRef(ref)) {
                            // this.f = new T();
                            param.connectionGraph.setArgEscape(value);
                        } else {
                            // p.f = new T();
                            if (ref.getReferenceValue() instanceof BlockParameter pv && pv.isEntryParameter()) {
                                // Object that `p` points to was created outside this method (e.g. `p` is a formal parameter)
                                // Set link from object in caller's context, via field, to the new value.
                                param.connectionGraph.addFieldEdge(pv, fieldOf);
                                param.connectionGraph.addPointsToEdge(fieldOf, value);
                            }
                        }
                    }
                } else if (handle instanceof StaticField) {
                    param.connectionGraph.setGlobalEscape(handle);
                    if (value instanceof NotNull nn) {
                        param.connectionGraph.addPointsToEdge(handle, nn.getInput());
                    } else {
                        param.connectionGraph.addPointsToEdge(handle, value);
                    }
                }
            }

            return null;
        }

        private boolean isThisRef(ReferenceHandle ref) {
            return ref.getReferenceValue() instanceof BlockParameter param
                && param.isEntryParameter() && param.getSlot() == Slot.this_();
        }

        @Override
        public Void visit(AnalysisContext param, Call node) {
            if (visitKnown(param, node) && node.getPointerValue() instanceof Executable) {
                param.escapeAnalysisState.addCall(element, node);
            }

            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Return node) {
            if (visitKnown(param, node)) {
                final Value value = node.getReturnValue();
                if (value instanceof New || value instanceof BlockParameter) {
                    param.connectionGraph.setArgEscape(value);
                } else if (value instanceof Call call && !isPrimitive(call.getType())) {
                    for (Value argument : call.getArguments()) {
                        param.connectionGraph.setArgEscape(argument);
                    }
                } else if (value instanceof Invoke.ReturnValue ret && !isPrimitive(ret.getType())) {
                    for (Value argument : ret.getInvoke().getArguments()) {
                        param.connectionGraph.setArgEscape(argument);
                    }
                }
            }

            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Throw node) {
            if (visitKnown(param, node)) {
                final Value value = node.getThrownValue();
                if (value instanceof New) {
                    // New allocations thrown assumed to escape as arguments
                    // TODO Could it be possible to only mark as argument escaping those that escape the method?
                    param.connectionGraph.setArgEscape(value);
                }
            }

            return null;
        }

        @Override
        public Void visit(AnalysisContext param, CheckCast node) {
            if (visitKnown(param, node)) {
                param.connectionGraph.addPointsToEdge(node, node.getInput());
            }

            return null;
        }

        @Override
        public Void visit(AnalysisContext param, BitCast node) {
            if (visitKnown(param, node)) {
                param.connectionGraph.addPointsToEdge(node, node.getInput());
            }

            return null;
        }

        @Override
        public Void visit(AnalysisContext param, BlockParameter node) {
            boolean known = visitKnown(param, node);
            if (known) {
                if (node.isEntryParameter() && Slot.this_() != node.getSlot()) {
                    // ParameterValue servers as an anchor for the summary information,
                    //   that will be generated when we finish analyzing the current method.
                    param.connectionGraph.addParameter(node);
                    param.connectionGraph.setArgEscape(node);
                } else {
                    param.connectionGraph.setNoEscape(node);
                }
            }

            return null;
        }

        @Override
        public Void visit(AnalysisContext param, ReferenceHandle node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Load node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, StaticField node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, StaticMethodElementHandle node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, IsEq node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, IsGe node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, IsGt node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, IsLe node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, IsLt node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, IsNe node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, VirtualMethodElementHandle node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, ConstructorElementHandle node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, InstanceFieldOf node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Truncate node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Extend node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Add node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Sub node) {
            visitKnown(param, node);
            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Select node) {
            visitKnown(param, node);
            return null;
        }

        // terminators

        @Override
        public Void visit(AnalysisContext param, If node) {
            if (visitKnown(param, node)) {
                node.getTrueBranch().getTerminator().accept(this, param);
                node.getFalseBranch().getTerminator().accept(this, param);
            }

            return null;
        }

        @Override
        public Void visit(AnalysisContext param, Goto node) {
            if (visitKnown(param, node)) {
                node.getResumeTarget().getTerminator().accept(this, param);
            }

            return null;
        }

        private EscapeValue defaultEscapeValue(AnalysisContext param, ClassObjectType type) {
            if (param.isSubtypeOfClass("java/lang/Thread", type) ||
                param.isSubtypeOfClass("java/lang/ThreadGroup", type)) {
                return EscapeValue.GLOBAL_ESCAPE;
            }

            return EscapeValue.NO_ESCAPE;
        }

        @Override
        public Void visitUnknown(AnalysisContext param, Action node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(AnalysisContext param, Terminator node) {
            if (visitUnknown(param, (Node) node)) {
                // process reachable successors
                int cnt = node.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    node.getSuccessor(i).getTerminator().accept(this, param);
                }
            }
            return null;
        }

        @Override
        public Void visitUnknown(AnalysisContext param, PointerValue node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(AnalysisContext param, Value node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        boolean visitKnown(AnalysisContext param, Node node) {
            param.addKnownType(node.getClass());
            return visitUnknown(param, node);
        }

        boolean visitUnknown(AnalysisContext param, Node node) {
            if (param.visited.add(node)) {
                boolean isNodeSupported = isSupported(param, node);

                if (node.hasPointerValueDependency()) {
                    final PointerValue dependency = node.getPointerValue();
                    checkSupport(isNodeSupported, dependency, param);
                    dependency.accept(this, param);
                }

                int cnt = node.getValueDependencyCount();
                for (int i = 0; i < cnt; i ++) {
                    final Value dependency = node.getValueDependency(i);
                    checkSupport(isNodeSupported, dependency, param);
                    dependency.accept(this, param);
                }

                if (node instanceof OrderedNode) {
                    Node dependency = ((OrderedNode) node).getDependency();
                    checkSupport(isNodeSupported, dependency, param);
                    if (dependency instanceof Action) {
                        ((Action) dependency).accept(this, param);
                    } else if (dependency instanceof Value) {
                        ((Value) dependency).accept(this, param);
                    } else if (dependency instanceof Terminator) {
                        ((Terminator) dependency).accept(this, param);
                    } else if (dependency instanceof PointerValue) {
                        ((PointerValue) dependency).accept(this, param);
                    }
                }

                return true;
            }

            return false;
        }

        private void checkSupport(boolean isSupported, Node node, AnalysisContext param) {
            if (!isSupported) {
                if (TRUE.equals(param.supported.get(node))) {
                    param.switchToUnsupported(node);
                    // Remove a node from visited if it switched from supported to unsupported.
                    // This way the unsupported new status can trickle back through its dependencies.
                    param.visited.remove(node);
                } else {
                    param.addUnsupported(node);
                }
            }
        }

        /**
         * Checks if a node is supported or not.
         * A node's support status might have been set by a dependency (value or control), if the dependency itself was unsupported.
         * In this case, irrespective of the node type, the node will remain unsupported.
         * If a node's support status is unknown, a node will be supported if its type is amongst supported types.
         * Otherwise, it will return false.
         */
        private boolean isSupported(AnalysisContext param, Node node) {
            if (node instanceof Literal) {
                return true;
            }

            final Boolean prev = param.supported.get(node);
            if (prev == null) {
                boolean supported = param.knownTypes.contains(node.getClass());
                param.setSupported(node, supported);
                return supported;
            }
            return prev.booleanValue();
        }

        private static boolean isPrimitive(ValueType type) {
            return type instanceof VoidType
                || type instanceof BooleanType
                || type instanceof NumericType;
        }
    }

    static final class AnalysisContext {
        final Set<Node> visited = new HashSet<>();
        final Map<Node, Boolean> supported = new HashMap<>();
        final Set<Class<?>> knownTypes = new HashSet<>(); // known supported types
        final EscapeAnalysisState escapeAnalysisState;
        final ConnectionGraph connectionGraph;
        final ClassContext bootstrapClassContext;

        AnalysisContext(EscapeAnalysisState escapeAnalysisState, ConnectionGraph connectionGraph, ClassContext bootstrapClassContext) {
            this.escapeAnalysisState = escapeAnalysisState;
            this.connectionGraph = connectionGraph;
            this.bootstrapClassContext = bootstrapClassContext;
            this.knownTypes.add(BlockEntry.class);
            this.knownTypes.add(BlockParameter.class);
        }

        void addKnownType(Class<?> type) {
            knownTypes.add(type);
        }

        void setSupported(Node node, boolean value) {
            this.supported.put(node, value);
        }

        boolean switchToUnsupported(Node node) {
            return this.supported.replace(node, true, false);
        }

        boolean addUnsupported(Node node) {
            return this.supported.putIfAbsent(node, false) == null;
        }

        private boolean isSubtypeOfClass(String name, ClassObjectType type) {
            return type.isSubtypeOf(bootstrapClassContext.findDefinedType(name).load().getObjectType());
        }

        public void process(ExecutableElement element, BasicBlock entryBlock) {
            entryBlock.getTerminator().accept(new AnalysisVisitor(element), this);

            // Incoming values for phi nodes can only be calculated upon finish.
            connectionGraph.resolveReturnedPhiValues();

            final List<New> notGlobalEscapeNewNodes = this.supported.entrySet().stream()
                .filter(e -> e.getKey() instanceof New && e.getValue())
                .filter(e -> connectionGraph.getEscapeValue(e.getKey()).notGlobalEscape())
                .map(e -> (New) e.getKey())
                .toList();

            connectionGraph.validateNewNodes(notGlobalEscapeNewNodes);
        }
    }
}
