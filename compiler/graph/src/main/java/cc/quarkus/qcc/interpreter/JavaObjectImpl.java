package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.FieldContainer;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

class JavaObjectImpl implements JavaObject {
    final VerifiedTypeDefinition definition;
    final FieldContainer fields;
    JavaObjectImpl(final VerifiedTypeDefinition definition) {
        this.definition = definition;
        fields = FieldContainer.forInstanceFieldsOf(definition);
    }

    public JavaClass getJavaClass() {
        return definition.getJavaClass();
    }
}
