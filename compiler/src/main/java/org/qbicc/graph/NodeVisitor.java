package org.qbicc.graph;

/**
 * A visitor that can visit any program node.
 */
public interface NodeVisitor<C, V, A, T, H> extends ValueVisitor<C, V>, ActionVisitor<C, A>, TerminatorVisitor<C, T>, PointerValueVisitor<C, H> {

    interface Delegating<C, V, A, T, H> extends NodeVisitor<C, V, A, T, H>, ValueVisitor.Delegating<C, V>, ActionVisitor.Delegating<C, A>, TerminatorVisitor.Delegating<C, T>, PointerValueVisitor.Delegating<C, H> {
        NodeVisitor<C, V, A, T, H> getDelegateNodeVisitor();

        default ActionVisitor<C, A> getDelegateActionVisitor() {
            return getDelegateNodeVisitor();
        }

        default TerminatorVisitor<C, T> getDelegateTerminatorVisitor() {
            return getDelegateNodeVisitor();
        }

        default ValueVisitor<C, V> getDelegateValueVisitor() {
            return getDelegateNodeVisitor();
        }

        default PointerValueVisitor<C, H> getDelegatePointerValueVisitor() {
            return getDelegateNodeVisitor();
        }
    }
}
