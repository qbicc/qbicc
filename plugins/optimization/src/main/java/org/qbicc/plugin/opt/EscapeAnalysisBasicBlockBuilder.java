package org.qbicc.plugin.opt;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Store;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.definition.element.FieldElement;

import java.util.List;

public class EscapeAnalysisBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public EscapeAnalysisBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value new_(ClassObjectType type) {
        final Value result = super.new_(type);

        // new T(...);
        // Default object to no escape
        EscapeAnalysis.get(ctxt).setNoEscape(getCurrentElement(), result);

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

    private void handleInstanceFieldOf(InstanceFieldOf result, ValueHandle handle, Node target) {
        if (target instanceof New) {
            EscapeAnalysis.get(ctxt).addFieldEdgeIfAbsent(getCurrentElement(), (New) target, result);
            EscapeAnalysis.get(ctxt).addPointsToEdgeIfAbsent(getCurrentElement(), handle, (New) target);
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

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        final Node result = super.store(handle, value, mode);

        if (handle instanceof StaticField) {
            // static T a = new T();
            EscapeAnalysis.get(ctxt).setGlobalEscape(getCurrentElement(), value);
        } else if (handle instanceof InstanceFieldOf && value instanceof New) {
            // p.f = new T(); // where p is a parameter
            EscapeAnalysis.get(ctxt).addPointsToEdgeIfAbsent(getCurrentElement(), handle, (New) value);
        }

        return result;
    }

    @Override
    public void startMethod(List<ParameterValue> arguments) {
        super.startMethod(arguments);
        arguments.forEach(arg -> EscapeAnalysis.get(ctxt).setArgEscape(getCurrentElement(), arg));
    }

    @Override
    public BasicBlock return_(Value value) {
        final BasicBlock result = super.return_(value);
        EscapeAnalysis.get(ctxt).setArgEscape(getCurrentElement(), value);
        return result;
    }

    @Override
    public void finish() {
        EscapeAnalysis.get(ctxt).methodExit(getCurrentElement());
        super.finish();
    }
}
