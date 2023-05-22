package org.qbicc.plugin.native_;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.type.StructType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.InstanceFieldElement;

public class StructMemberAccessBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public StructMemberAccessBasicBlockBuilder(FactoryContext context, BasicBlockBuilder delegate) {
        super(delegate);
    }

    public Value instanceFieldOf(Value instancePointer, InstanceFieldElement field) {
        ValueType valueType = instancePointer.getPointeeType();
        if (valueType instanceof StructType) {
            return memberOf(instancePointer, ((StructType) valueType).getMember(field.getName()));
        }
        return super.instanceFieldOf(instancePointer, field);
    }
}
