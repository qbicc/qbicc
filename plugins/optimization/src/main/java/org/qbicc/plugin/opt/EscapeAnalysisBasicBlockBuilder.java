package org.qbicc.plugin.opt;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

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
        final ValueHandle result = super.instanceFieldOf(handle, field);

        if (handle instanceof ReferenceHandle) {
            // T a = new T(...);
            // To get represent the GC from 'a' to 'new T(...)',
            // we hijack future 'a' references to fix the pointer.
            // When 'a.x' is accessed, we fix the pointer from 'a' to 'new T(...)'.
            final ReferenceHandle refHandle = (ReferenceHandle) handle;
            final Node target = refHandle.getReferenceValue();
            handleReference(handle, result, target);
        }

        return result;
    }

    private void handleReference(ValueHandle handle, ValueHandle field, Node target) {
        // TODO handle target being ParameterValue
        if (target instanceof New) {
            EscapeAnalysis.get(ctxt).addFieldEdgeIfAbsent(getCurrentElement(), (New) target, field);
            EscapeAnalysis.get(ctxt).addPointsToEdgeIfAbsent(getCurrentElement(), handle, (New) target);
        } else if (target instanceof OrderedNode) {
            handleReference(handle, field, ((OrderedNode) target).getDependency());
        }
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        final Node result = super.store(handle, value, mode);
        return result;
    }

    @Override
    public void finish() {
        EscapeAnalysis.get(ctxt).methodExit(getCurrentElement());
        super.finish();
    }
}
