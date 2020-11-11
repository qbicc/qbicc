package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.graph.literal.RealTypeIdLiteral;
import cc.quarkus.qcc.type.definition.FieldContainer;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

class VmObjectImpl implements VmObject {
    final ValidatedTypeDefinition definition;
    final FieldContainer fields;

    VmObjectImpl(final ValidatedTypeDefinition definition) {
        this.definition = definition;
        fields = FieldContainer.forInstanceFieldsOf(definition);
    }

    public RealTypeIdLiteral getObjectType() {
        return (RealTypeIdLiteral) definition.getTypeId();
    }

    FieldContainer getFields() {
        return fields;
    }
}
