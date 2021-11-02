package org.qbicc.plugin.native_;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.FieldElement;

public class StructMemberAccessBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public StructMemberAccessBasicBlockBuilder(CompilationContext context, BasicBlockBuilder delegate) {
        super(delegate);
        ctxt = context;
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        ValueType valueType = instance.getValueType();
        if (valueType instanceof CompoundType) {
            return memberOf(instance, ((CompoundType) valueType).getMember(field.getName()));
        }
        return super.instanceFieldOf(instance, field);
    }
}
