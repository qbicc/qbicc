package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.FieldSet;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

final class JavaClassImpl extends JavaObjectImpl implements JavaClass {
    final ValidatedTypeDefinition definition;
    final FieldSet instanceFields;

    JavaClassImpl(final JavaVMImpl vm, final ValidatedTypeDefinition definition) {
        super(vm.classClass.validate());
        this.definition = definition;
        instanceFields = new FieldSet(definition, false);
    }

    JavaClassImpl(final JavaVMImpl vm, final ValidatedTypeDefinition definition, boolean ignoredClassClass) {
        // Class.class
        super(definition);
        this.definition = definition;
        instanceFields = fields.getFieldSet();
    }

    public ValidatedTypeDefinition getTypeDefinition() {
        return definition;
    }
}
