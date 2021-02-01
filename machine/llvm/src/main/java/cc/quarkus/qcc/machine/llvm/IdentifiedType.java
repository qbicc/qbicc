package cc.quarkus.qcc.machine.llvm;

public interface IdentifiedType extends Commentable {
    LLValue asTypeRef();

    IdentifiedType type(LLValue type);

    IdentifiedType comment(String comment);
}
