package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface ResolvedMethodDefinition extends VerifiedMethodDefinition {
    default ResolvedMethodDefinition verify() {
        return this;
    }

    default ResolvedMethodDefinition resolve() {
        return this;
    }

    MethodGraph getMethodGraph() throws IllegalArgumentException;
}
