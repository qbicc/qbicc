package cc.quarkus.qcc.machine.llvm;

/**
 * Something which can receive LLVM source comments.
 */
public interface Commentable {
    /**
     * Add a comment.
     *
     * @param comment the comment text
     * @return this instance
     */
    Commentable comment(String comment);
}
