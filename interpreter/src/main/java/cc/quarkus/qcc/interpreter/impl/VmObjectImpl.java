package cc.quarkus.qcc.interpreter.impl;

import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.type.PhysicalObjectType;
import cc.quarkus.qcc.type.definition.FieldContainer;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

class VmObjectImpl implements VmObject {
    final ValidatedTypeDefinition definition;
    final FieldContainer fields;

    VmObjectImpl(final ValidatedTypeDefinition definition) {
        this.definition = definition;
        fields = FieldContainer.forInstanceFieldsOf(definition);
    }

    public PhysicalObjectType getObjectType() {
        return definition.getClassType();
    }

    FieldContainer getFields() {
        return fields;
    }
}
