package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface DefinedMethodBody {
    DefinedMethodDefinition getMethodDefinition();

    VerifiedMethodBody verify() throws VerifyFailedException;
}
