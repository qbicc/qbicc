package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface PreparedTypeDefinition extends ResolvedTypeDefinition {
    default PreparedTypeDefinition verify() {
        return this;
    }

    default PreparedTypeDefinition resolve() {
        return this;
    }

    default PreparedTypeDefinition prepare() {
        return this;
    }

    PreparedTypeDefinition getSuperClass();

    PreparedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

    InitializedTypeDefinition initialize() throws InitializationFailedException;
}
