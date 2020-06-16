package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface VerifiedMethodDefinition extends DefinedMethodDefinition {
    default VerifiedMethodDefinition verify() {
        return this;
    }

    boolean hasMethodBody();

    ResolvedMethodDefinition resolve();
}
