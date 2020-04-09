package cc.quarkus.qcc.metaprogram.jvm.signature;

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

    static ClassDeclarationSignature parseClassDeclarationSignature(String signature) {
        return Parsing.parseClassDeclarationSignature(signature);
    }

    static ClassDeclarationSignature getRoot() {
        return RootClassDeclarationSignature.INSTANCE;
    }
}
