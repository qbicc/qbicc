package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.descriptor.FieldDescriptor;

public interface FieldDefinition extends FieldDescriptor {
    TypeDefinition getTypeDefinition();
    String getName();
}
