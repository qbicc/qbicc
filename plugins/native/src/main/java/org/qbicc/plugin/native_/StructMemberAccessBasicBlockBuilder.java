package org.qbicc.plugin.native_;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.CompoundType;
import org.qbicc.type.definition.element.FieldElement;

public class StructMemberAccessBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public StructMemberAccessBasicBlockBuilder(CompilationContext context, BasicBlockBuilder delegate) {
        super(delegate);
        ctxt = context;
    }

    public ValueHandle referenceHandle(Value reference) {
        if (reference.getType() instanceof CompoundType) {
            return reference.getValueHandle();
        }
        return super.referenceHandle(reference);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        if (instance.getValueType() instanceof CompoundType) {
            Layout layout = Layout.get(ctxt);
            return memberOf(instance, layout.getInstanceLayoutInfo(field.getEnclosingType()).getMember(field));
        }
        return super.instanceFieldOf(instance, field);
    }
}
