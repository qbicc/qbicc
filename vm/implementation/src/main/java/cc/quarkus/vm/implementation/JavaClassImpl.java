package cc.quarkus.vm.implementation;

import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import cc.quarkus.vm.api.JavaClass;

final class JavaClassImpl extends JavaObjectImpl implements JavaClass {
    final VerifiedTypeDefinition definition;
    final FieldContainer staticFields;
    final FieldSet instanceFields;

    JavaClassImpl(final JavaVMImpl vm, final VerifiedTypeDefinition definition) {
        super(vm.getClassClass());
        this.definition = definition;
        staticFields = new FieldContainer(definition, new FieldSet(definition, true));
        instanceFields = new FieldSet(definition, false);
    }

    JavaClassImpl(final JavaVMImpl vm, final VerifiedTypeDefinition definition, boolean ignoredClassClass) {
        // Class.class
        super(new FieldContainer(definition, new FieldSet(definition, false)));
        this.definition = definition;
        staticFields = new FieldContainer(definition, new FieldSet(definition, true));
        instanceFields = fields.fieldSet;
    }

    public VerifiedTypeDefinition getTypeDefinition() {
        return definition;
    }
}
