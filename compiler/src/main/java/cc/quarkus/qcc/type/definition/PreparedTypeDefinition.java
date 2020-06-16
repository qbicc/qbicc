package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface PreparedTypeDefinition extends VerifiedTypeDefinition {
    default PreparedTypeDefinition verify() {
        return this;
    }

    default PreparedTypeDefinition prepare() {
        return this;
    }

    PreparedTypeDefinition getSuperclass();

    PreparedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

    InitializedTypeDefinition initialize() throws InitializationFailedException;
}
