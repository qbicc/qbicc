package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface InitializedTypeDefinition extends PreparedTypeDefinition {
    default InitializedTypeDefinition verify() {
        return this;
    }

    default InitializedTypeDefinition prepare() {
        return this;
    }

    default InitializedTypeDefinition initialize() {
        return this;
    }

    InitializedTypeDefinition getSuperclass();

    InitializedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;
}
