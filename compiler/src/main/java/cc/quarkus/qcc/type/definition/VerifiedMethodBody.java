package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface VerifiedMethodBody extends DefinedMethodBody {
    default VerifiedMethodBody verify() {
        return this;
    }

    ResolvedMethodBody resolve() throws ResolutionFailedException;
}
