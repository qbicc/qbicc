package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.graph.literal.RealTypeIdLiteral;
import cc.quarkus.qcc.type.definition.FieldContainer;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

class JavaObjectImpl implements JavaObject {
    final ValidatedTypeDefinition definition;
    final FieldContainer fields;

    JavaObjectImpl(final ValidatedTypeDefinition definition) {
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
