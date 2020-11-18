package cc.quarkus.qcc.type.generic;

/**
 *
 */
public interface ClassDeclarationSignature {
    int getTypeParameterCount();

    TypeParameter getTypeParameter(int index) throws IndexOutOfBoundsException;

    boolean hasSuperclass();

    ClassTypeSignature getSuperclass();

    int getInterfaceCount();

    ClassTypeSignature getInterface(int index) throws IndexOutOfBoundsException;

    static ClassDeclarationSignature parseClassDeclarationSignature(ParsingCache cache, String signature) {
        return Parsing.parseClassDeclarationSignature(cache, signature);
    }

    static ClassDeclarationSignature getRoot() {
        return RootClassDeclarationSignature.INSTANCE;
    }
}
