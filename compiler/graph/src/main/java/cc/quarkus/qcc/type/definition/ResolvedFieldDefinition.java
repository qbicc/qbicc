package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;

public interface ResolvedFieldDefinition extends DefinedFieldDefinition, FieldDescriptor {
    default ResolvedFieldDefinition resolve() {
        return this;
    }

    ResolvedTypeDefinition getEnclosingTypeDefinition();

    Type getType();
}
