package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.FieldSet;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

final class VmClassImpl extends VmObjectImpl implements VmClass {
    final ValidatedTypeDefinition definition;
    final FieldSet instanceFields;

    VmClassImpl(final VmImpl vm, final ValidatedTypeDefinition definition) {
        super(vm.classClass.validate());
        this.definition = definition;
        instanceFields = new FieldSet(definition, false);
    }

    VmClassImpl(final VmImpl vm, final ValidatedTypeDefinition definition, boolean ignoredClassClass) {
        // Class.class
        super(definition);
        this.definition = definition;
        instanceFields = fields.getFieldSet();
    }

    public ValidatedTypeDefinition getTypeDefinition() {
        return definition;
    }
}
