package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.FieldSet;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

final class JavaClassImpl extends JavaObjectImpl implements JavaClass {
    final VerifiedTypeDefinition definition;
    final FieldSet instanceFields;

    JavaClassImpl(final JavaVMImpl vm, final VerifiedTypeDefinition definition) {
        super(vm.classClass.verify());
        this.definition = definition;
        instanceFields = new FieldSet(definition, false);
    }

    JavaClassImpl(final JavaVMImpl vm, final VerifiedTypeDefinition definition, boolean ignoredClassClass) {
        // Class.class
        super(definition);
        this.definition = definition;
        instanceFields = fields.getFieldSet();
    }

    public VerifiedTypeDefinition getTypeDefinition() {
        return definition;
    }
}
