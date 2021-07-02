package org.qbicc.plugin.opt.ea;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Action;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Call;
import org.qbicc.graph.CastValue;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Executable;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Store;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

import static java.lang.Boolean.TRUE;

public final class EscapeAnalysisIntraMethodBuilder extends DelegatingBasicBlockBuilder  {
    private final EscapeAnalysisState escapeAnalysisState;
    private final ConnectionGraph connectionGraph;
    private final ClassContext bootstrapClassContext;
    private final SupportContext supportContext;

    public EscapeAnalysisIntraMethodBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.connectionGraph = new ConnectionGraph(getCurrentElement().toString());
        this.escapeAnalysisState = EscapeAnalysisState.get(ctxt);
        this.bootstrapClassContext = ctxt.getBootstrapClassContext();
        this.supportContext = new SupportContext(getCurrentElement().toString());
    }

    @Override
    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        final New result = (New) super.new_(type, typeId, size, align);
        connectionGraph.trackNew(result, defaultEscapeValue(type));
        return supports(result);
    }

    private EscapeValue defaultEscapeValue(ClassObjectType type) {
        if (isSubtypeOfClass("java/lang/Thread", type) ||
            isSubtypeOfClass("java/lang/ThreadGroup", type)) {
            return EscapeValue.GLOBAL_ESCAPE;
        }

        return EscapeValue.NO_ESCAPE;
    }

    private boolean isSubtypeOfClass(String name, ClassObjectType type) {
        return type.isSubtypeOf(bootstrapClassContext.findDefinedType(name).load().getType());
    }

    @Override
    public ValueHandle instanceFieldOf(ValueHandle handle, FieldElement field) {
        final InstanceFieldOf result = (InstanceFieldOf) super.instanceFieldOf(handle, field);

        // T a = new T(...);
        // To get represent the GC from 'a' to 'new T(...)',
        // we hijack future 'a' references to fix the pointer.
        // When 'a.x' is accessed, we fix the pointer from 'a' to 'new T(...)'.
        handleInstanceFieldOf(result, handle, handle);

        return supports(result);
    }

    @Override
    public Node store(ValueHandle handle, Value value, WriteAccessMode mode) {
        final Node result = super.store(handle, value, mode);

        if (handle instanceof StaticField) {
            // static T a = ...
            if (value instanceof NotNull nn) {
                connectionGraph.trackStoreStaticField(handle, nn.getInput());
            } else {
                connectionGraph.trackStoreStaticField(handle, value);
            }
        } else if (handle instanceof InstanceFieldOf fieldOf && value instanceof New) {
            if (isThisHandle(fieldOf)) {
                // this.f = new T();
                connectionGraph.trackStoreThisField(value);
            } else {
                // p.f = new T(); // where p is a parameter
                connectionGraph.fixEdgesNew(handle, (New) value);
            }
        } else if (handle instanceof LocalVariable && value instanceof New) {
            connectionGraph.trackLocalNew((LocalVariable) handle, (New) value);
        }

        return supports(result);
    }

    private boolean isThisHandle(InstanceFieldOf fieldOf) {
        return fieldOf.getValueHandle() instanceof ReferenceHandle ref
            && ref.getReferenceValue() instanceof ParameterValue param
            && "this".equals(param.getLabel());
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        final Value result = super.call(target, arguments);

        if (target instanceof Executable) {
            escapeAnalysisState.trackCall(getCurrentElement(), (Call) result);
        }

        return supports(result);
    }

    @Override
    public ValueHandle constructorOf(Value instance, ConstructorElement constructor, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return supports(super.constructorOf(instance, constructor, callSiteDescriptor, callSiteType));
    }

    @Override
    public void startMethod(List<ParameterValue> arguments) {
        super.startMethod(arguments);
        escapeAnalysisState.trackMethod(getCurrentElement(), this.connectionGraph);
        connectionGraph.trackParameters(arguments);
    }

    @Override
    public BasicBlock return_(Value value) {
        final BasicBlock result = super.return_(value);

        // Skip primitive values truncated, they are not objects
        if (!(value instanceof Truncate)) {
            connectionGraph.trackReturn(value);
        }

        return supports(result.getTerminator(), result);
    }

    @Override
    public BasicBlock throw_(Value value) {
        final BasicBlock result = super.throw_(value);

        if (value instanceof New) {
            connectionGraph.trackThrowNew((New) value);
        }

        return result;
    }

    @Override
    public Value checkcast(Value value, Value toType, Value toDimensions, CheckCast.CastType kind, ObjectType expectedType) {
        final CastValue result = (CastValue) super.checkcast(value, toType, toDimensions, kind, expectedType);
        connectionGraph.trackCast(result);
        return supports(result);
    }

    @Override
    public Value bitCast(Value value, WordType toType) {
        final CastValue result = (CastValue) super.bitCast(value, toType);
        connectionGraph.trackCast(result);
        return supports(result);
    }

    @Override
    public Value load(ValueHandle handle, ReadAccessMode accessMode) {
        return supports(super.load(handle, accessMode));
    }

    @Override
    public ValueHandle referenceHandle(Value reference) {
        return supports(super.referenceHandle(reference));
    }

    @Override
    public BasicBlock if_(Value condition, BlockLabel trueTarget, BlockLabel falseTarget) {
        final BasicBlock result = super.if_(condition, trueTarget, falseTarget);
        return supports(result.getTerminator(), result);
    }

    @Override
    public Value isEq(Value v1, Value v2) {
        return supports(super.isEq(v1, v2));
    }

    @Override
    public Value sub(Value v1, Value v2) {
        return supports(super.sub(v1, v2));
    }

    @Override
    public Value add(Value v1, Value v2) {
        return supports(super.add(v1, v2));
    }

    @Override
    public Value truncate(Value value, WordType toType) {
        return supports(super.truncate(value, toType));
    }

    @Override
    public Value extend(Value value, WordType toType) {
        return supports(super.extend(value, toType));
    }

    @Override
    public ValueHandle virtualMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return supports(super.virtualMethodOf(instance, method, callSiteDescriptor, callSiteType));
    }

    @Override
    public BasicBlock goto_(BlockLabel resumeLabel) {
        final BasicBlock result = super.goto_(resumeLabel);
        return supports(result.getTerminator(), result);
    }

    @Override
    public void finish() {
        super.finish();
        // Incoming values for phi nodes can only be calculated upon finish.
        connectionGraph.resolveReturnedPhiValues();

        // Verify support for escape analysis
        supportContext.process();

        final List<New> supported = supportContext.supported.entrySet().stream()
            .filter(e -> e.getKey() instanceof New && e.getValue())
            .filter(e -> connectionGraph.getEscapeValue(e.getKey()).notGlobalEscape())
            .map(e -> (New) e.getKey())
            .toList();

        connectionGraph.validateNewNodes(supported);
    }

    private <T extends Node> T supports(T value) {
        supportContext.addType(value.getClass());
        return value;
    }

    private BasicBlock supports(Terminator terminator, BasicBlock block) {
        supportContext.addToQueue(terminator);
        supportContext.addType(terminator.getClass());
        return block;
    }

    private void handleInstanceFieldOf(InstanceFieldOf result, ValueHandle handle, Node target) {
        if (target instanceof New) {
            connectionGraph.fixEdgesField((New) target, handle, result);
        } else if (target instanceof ParameterValue) {
            connectionGraph.fixEdgesParameterValue((ParameterValue) target, result);
        } else if (target instanceof Store) {
            final Value value = ((Store) target).getValue();
            if (value instanceof New) {
                handleInstanceFieldOf(result, handle, value);
            } else {
                handleInstanceFieldOf(result, handle, target.getValueHandle());
            }
        } else if (target instanceof InstanceFieldOf) {
            handleInstanceFieldOf(result, handle, target.getValueHandle());
        } else if (target instanceof ReferenceHandle) {
            handleInstanceFieldOf(result, handle, ((ReferenceHandle) target).getReferenceValue());
        } else if (target instanceof OrderedNode) {
            handleInstanceFieldOf(result, handle, ((OrderedNode) target).getDependency());
        }
    }

    static final class SupportContext {
        final Set<Node> visited = new HashSet<>();
        final Map<Node, Boolean> supported = new HashMap<>();
        final Set<Class<?>> types = new HashSet<>(); // supported type
        final Queue<Terminator> terminatorQueue = new ArrayDeque<>();
        final String name;

        SupportContext(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "SupportContext{" +
                "name='" + name + '\'' +
                '}';
        }

        void addType(Class<?> type) {
            types.add(type);
        }

        void addToQueue(Terminator terminator) {
            terminatorQueue.add(terminator);
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

        public void process() {
            Terminator terminator;
            final SupportVisitor visitor = new SupportVisitor();
            while ((terminator = terminatorQueue.poll()) != null) {
                terminator.accept(visitor, this);
            }
        }
    }

    static final class SupportVisitor implements NodeVisitor<SupportContext, Void, Void, Void, Void> {
        @Override
        public Void visitUnknown(SupportContext param, Action node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(SupportContext param, Terminator node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(SupportContext param, ValueHandle node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        @Override
        public Void visitUnknown(SupportContext param, Value node) {
            visitUnknown(param, (Node) node);
            return null;
        }

        void visitUnknown(SupportContext param, Node node) {
            if (param.visited.add(node)) {
                boolean isNodeSupported = isSupported(param, node);

                if (node.hasValueHandleDependency()) {
                    final ValueHandle dependency = node.getValueHandle();
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
                    } else if (dependency instanceof ValueHandle) {
                        ((ValueHandle) dependency).accept(this, param);
                    }
                }
            }
        }

        private void checkSupport(boolean isSupported, Node node, SupportContext param) {
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
        private boolean isSupported(SupportContext param, Node node) {
            final Boolean prev = param.supported.get(node);
            if (prev == null) {
                boolean supported = param.types.contains(node.getClass());
                param.setSupported(node, supported);
                return supported;
            }
            return prev.booleanValue();
        }
    }
}
