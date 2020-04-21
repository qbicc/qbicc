package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.TypeDefinition;

public class ObjectReference {

    public ObjectReference(TypeDefinition typeDefinition) {
        this.typeDefinition = typeDefinition;
    }

    public TypeDefinition getTypeDefinition() {
        return this.typeDefinition;
    }

    private final TypeDefinition typeDefinition;
}
