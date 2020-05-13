package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;

public interface FieldDefinition<V extends QType> extends FieldDescriptor<V> {
    TypeDefinition getTypeDefinition();
    String getName();
}
