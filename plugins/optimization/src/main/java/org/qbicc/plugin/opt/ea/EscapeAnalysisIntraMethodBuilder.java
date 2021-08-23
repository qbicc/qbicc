package org.qbicc.plugin.opt.ea;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Call;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Executable;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Store;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.definition.element.FieldElement;

public final class EscapeAnalysisIntraMethodBuilder extends DelegatingBasicBlockBuilder  {
    private final EscapeAnalysisState escapeAnalysisState;
    private final ConnectionGraph connectionGraph;

    public EscapeAnalysisIntraMethodBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.connectionGraph = new ConnectionGraph(getCurrentElement().toString());
        this.escapeAnalysisState = EscapeAnalysisState.get(ctxt);
        this.escapeAnalysisState.trackMethod(getCurrentElement(), this.connectionGraph);
    }

    @Override
    public Value new_(ClassObjectType type) {
        final New result = (New) super.new_(type);

        connectionGraph.trackNew(result, type, getCurrentElement());

        return result;
    }

    @Override
    public ValueHandle instanceFieldOf(ValueHandle handle, FieldElement field) {
        final InstanceFieldOf result = (InstanceFieldOf) super.instanceFieldOf(handle, field);

        // T a = new T(...);
        // To get represent the GC from 'a' to 'new T(...)',
        // we hijack future 'a' references to fix the pointer.
        // When 'a.x' is accessed, we fix the pointer from 'a' to 'new T(...)'.
        handleInstanceFieldOf(result, handle, handle);

        return result;
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        final Node result = super.store(handle, value, mode);

        if (handle instanceof StaticField) {
            // static T a = new T();
            connectionGraph.trackStoreStaticField(value);
        } else if (handle instanceof InstanceFieldOf && value instanceof New) {
            // p.f = new T(); // where p is a parameter
            connectionGraph.fixEdgesNew(handle, (New) value);
        } else if (handle instanceof LocalVariable && value instanceof New) {
            connectionGraph.trackLocalNew((LocalVariable) handle, (New) value);
        }

        return result;
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        final Value result = super.call(target, arguments);

        if (target instanceof Executable) {
            escapeAnalysisState.trackCall(getCurrentElement(), (Call) result);
        }

        return result;
    }

    @Override
    public void startMethod(List<ParameterValue> arguments) {
        super.startMethod(arguments);
        connectionGraph.trackParameters(arguments);
    }

    @Override
    public BasicBlock return_(Value value) {
        final BasicBlock result = super.return_(value);

        // Skip primitive values truncated, they are not objects
        if (!(value instanceof Truncate)) {
            connectionGraph.trackReturn(value);
        }

        return result;
    }

    @Override
    public void finish() {
        doReachabilityAnalysis();
        super.finish();
    }

    void doReachabilityAnalysis() {
        // TODO: Use ByPass function to eliminate all deferred edges in the CG

        // TODO: 1. compute set of nodes reachable from GlobalEscape node(s)

        // TODO 2. Compute set of nodes reachable from ArgEscape (nodes), but not any GlobalEscape node

        // TODO: 3. compute set of nodes not reachable from GlobalEscape or ArgEscape

        // TODO double check if propagating arg escape should happen here or in inter analysis
        connectionGraph.propagateArgEscape();
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
}
