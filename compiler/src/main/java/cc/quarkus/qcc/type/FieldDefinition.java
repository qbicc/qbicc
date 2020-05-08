package cc.quarkus.qcc.type;

public interface FieldDefinition<V> extends FieldDescriptor<V> {
    TypeDefinition getTypeDefinition();
    String getName();
}
