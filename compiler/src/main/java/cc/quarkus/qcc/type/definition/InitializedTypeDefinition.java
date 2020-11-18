package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface InitializedTypeDefinition extends PreparedTypeDefinition {
    default InitializedTypeDefinition validate() {
        return this;
    }

    default InitializedTypeDefinition resolve() {
        return this;
    }

    default InitializedTypeDefinition prepare() {
        return this;
    }

    default InitializedTypeDefinition initialize() {
        return this;
    }

    InitializedTypeDefinition getSuperClass();

    InitializedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;
}
