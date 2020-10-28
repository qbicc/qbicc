package cc.quarkus.qcc.machine.llvm;

/**
 *
 */
public interface Metable extends Commentable {
    Metable meta(String name, LLValue data);
}
