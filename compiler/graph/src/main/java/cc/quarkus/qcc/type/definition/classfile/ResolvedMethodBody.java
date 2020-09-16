package cc.quarkus.qcc.type.definition.classfile;

final class ResolvedMethodBody {
    private final VerifiedMethodBody verifiedMethodBody;

    ResolvedMethodBody(final VerifiedMethodBody verifiedMethodBody) {
        this.verifiedMethodBody = verifiedMethodBody;
    }

}
