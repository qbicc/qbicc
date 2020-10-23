package cc.quarkus.qcc.graph;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.literal.BlockLiteral;

/**
 * A visitor over a graph of terminator nodes.  Terminator nodes form a directed graph which may contain cycles.
 */
public interface TerminatorVisitor<T, R> {
    default R visitUnknown(T param, Terminator node) {
        return null;
    }

    default R visit(T param, Goto node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, If node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Jsr node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Ret node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Return node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Switch node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Throw node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Try node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ValueReturn node) {
        return visitUnknown(param, node);
    }

    // Errors

    default R visit(T param, ClassCastErrorNode node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, NoSuchMethodErrorNode node) {
        return visitUnknown(param, node);
    }

    interface Delegating<T, R> extends TerminatorVisitor<T, R> {
        TerminatorVisitor<T, R> getDelegateTerminatorVisitor();

        default R visitUnknown(T param, Terminator node) {
            return node.accept(getDelegateTerminatorVisitor(), param);
        }

        default R visit(T param, Goto node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, If node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Jsr node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Ret node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Return node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Switch node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Throw node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Try node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, ValueReturn node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, ClassCastErrorNode node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, NoSuchMethodErrorNode node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }
    }

    /**
     * A terminator visitor base interface which recursively copies the given terminators.
     * <p>
     * To transform certain terminator types, override the specific {@code visit()} method for that type.
     *
     * @param <T> the parameter type
     * @deprecated Replace with {@link Node.Copier}.
     */
    @Deprecated
    interface Copying<T> extends TerminatorVisitor<T, BasicBlock> {

        /**
         * Get the block builder to use to construct copied nodes.  This method should return the same builder instance
         * on every call with the same parameter.
         *
         * @param param the parameter which was passed in to the {@link #copy} method
         * @return the builder
         */
        BasicBlockBuilder getBuilder(T param);

        /**
         * Get the visitor entry point to use for copying a terminator.  The visitor should ultimately delegate back
         * to this visitor.
         *
         * @return the copying visitor
         */
        default TerminatorVisitor<T, BasicBlock> getCopyingTerminatorVisitor() {
            return this;
        }

        /**
         * Entry point to copy the given terminator.  Subclasses may introduce caching or mapping at this point.  The default
         * implementation always makes a copy and returns the terminated block.
         *
         * @param param    the parameter to pass to the visitor methods
         * @param original the terminator to copy
         * @return the terminated block
         */
        default BasicBlock copy(T param, Terminator original) {
            int cnt = original.getBasicDependencyCount();
            for (int i = 0; i < cnt; i++) {
                copy(param, original.getBasicDependency(i));
            }
            BasicBlockBuilder builder = getBuilder(param);
            int oldLine = builder.setLineNumber(original.getSourceLine());
            int oldBci = builder.setBytecodeIndex(original.getBytecodeIndex());
            try {
                return original.accept(getCopyingTerminatorVisitor(), param);
            } finally {
                builder.setBytecodeIndex(oldBci);
                builder.setLineNumber(oldLine);
            }
        }

        /**
         * Copy the label for the given block.
         *
         * @param param the parameter to pass to the visitor methods
         * @param block the block to copy
         * @return the copied label
         */
        BlockLabel copy(T param, BasicBlock block);

        private List<BlockLabel> copy(T param, List<BlockLabel> blocks) {
            List<BlockLabel> copies = new ArrayList<>();
            for (BlockLabel block : blocks) {
                copies.add(copy(param, BlockLabel.getTargetOf(block)));
            }
            return copies;
        }

        /**
         * Make a copy of a dependency node.
         *
         * @param param the parameter to pass to the visitor methods
         * @param original the node to copy
         * @return the copied node
         */
        Node copy(T param, Node original);

        /**
         * Copy the given value.  The value would be given as an argument to a terminator.
         *
         * @param param the parameter to pass to the visitor methods
         * @param original the value to copy
         * @return the copied value
         */
        Value copy(T param, Value original);

        /**
         * Copy the given triable operation.  The value would be given as an argument to a {@code try} operation.
         *
         * @param param the parameter to pass to the visitor methods
         * @param original the operation to copy
         * @return the copied operation
         */
        Triable copy(T param, Triable original);

        default BasicBlock visit(T param, Goto node) {
            return getBuilder(param).goto_(copy(param, node.getResumeTarget()));
        }

        default BasicBlock visit(T param, If node) {
            return getBuilder(param).if_(copy(param, node.getCondition()), copy(param, node.getTrueBranch()), copy(param, node.getFalseBranch()));
        }

        default BasicBlock visit(T param, Jsr node) {
            return getBuilder(param).jsr(copy(param, node.getResumeTarget()), (BlockLiteral) copy(param, node.getReturnAddressValue()));
        }

        default BasicBlock visit(T param, Ret node) {
            return getBuilder(param).ret(copy(param, node.getReturnAddressValue()));
        }

        default BasicBlock visit(T param, Return node) {
            return getBuilder(param).return_();
        }

        default BasicBlock visit(T param, Switch node) {
            int cnt = node.getNumberOfValues();
            BlockLabel[] targetsCopy = new BlockLabel[cnt];
            for (int i = 0; i < cnt; i ++) {
                targetsCopy[i] = copy(param, node.getTargetForIndex(i));
            }
            return getBuilder(param).switch_(copy(param, node.getSwitchValue()), node.getValues(), targetsCopy, copy(param, node.getDefaultTarget()));
        }

        default BasicBlock visit(T param, Throw node) {
            return getBuilder(param).throw_(copy(param, node.getThrownValue()));
        }

        default BasicBlock visit(T param, Try node) {
            return getBuilder(param).try_(copy(param, node.getDelegateOperation()), node.getCatchTypeIds(), copy(param, node.getCatchTargetLabels()), copy(param, node.getResumeTarget()));
        }

        default BasicBlock visit(T param, ValueReturn node) {
            return getBuilder(param).return_(copy(param, node.getReturnValue()));
        }

        default BasicBlock visit(T param, ClassCastErrorNode node) {
            return getBuilder(param).classCastException(copy(param, node.getFromType()), copy(param, node.getToType()));
        }

        default BasicBlock visit(T param, NoSuchMethodErrorNode node) {
            return getBuilder(param).noSuchMethodError(node.getOwner(), node.getDescriptor(), node.getName());
        }
    }
}
