package org.qbicc.graph;

/**
 * A visitor that can visit any program node.
 */
public interface NodeVisitor<C, V, A, T> extends ValueVisitor<C, V>, ActionVisitor<C, A>, TerminatorVisitor<C, T> {

    interface Delegating<C, V, A, T> extends NodeVisitor<C, V, A, T>, ValueVisitor.Delegating<C, V>, ActionVisitor.Delegating<C, A>, TerminatorVisitor.Delegating<C, T> {
        NodeVisitor<C, V, A, T> getDelegateNodeVisitor();

        default ActionVisitor<C, A> getDelegateActionVisitor() {
            return getDelegateNodeVisitor();
        }

        default TerminatorVisitor<C, T> getDelegateTerminatorVisitor() {
            return getDelegateNodeVisitor();
        }

        default ValueVisitor<C, V> getDelegateValueVisitor() {
            return getDelegateNodeVisitor();
        }
    }
}
