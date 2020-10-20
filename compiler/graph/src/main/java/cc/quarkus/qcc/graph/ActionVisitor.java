package cc.quarkus.qcc.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * A visitor over a graph of non-value action nodes.  Non-value action nodes form a directed acyclic graph (DAG).
 */
public interface ActionVisitor<T, R> {
    default R visitUnknown(T param, Action node) {
        return null;
    }

    default R visit(T param, ArrayElementWrite node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, BlockEntry node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceFieldWrite node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceInvocation node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MonitorEnter node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MonitorExit node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticFieldWrite node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticInvocation node) {
        return visitUnknown(param, node);
    }

    /**
     * An action visitor base interface which recursively copies the given actions.  Note that an action's copy may
     * be transformed to any node type.
     * <p>
     * To transform certain action types, override the specific {@code visit()} method for that type.
     *
     * @param <T> the parameter type
     */
    interface Copying<T> extends ActionVisitor<T, Node> {

        /**
         * Get the block builder to use to construct copied nodes.  This method should return the same builder instance
         * on every call with the same parameter.
         *
         * @param param the parameter which was passed in to the {@link #copy} method
         * @return the builder
         */
        BasicBlockBuilder getBuilder(T param);

        /**
         * Entry point to copy the given value.  Subclasses may introduce caching or mapping at this point.  The default
         * implementation always makes a copy and returns it.
         *
         * @param param    the parameter to pass to the visitor methods
         * @param original the node to copy
         * @return the copy
         */
        default Node copy(T param, Action original) {
            int cnt = original.getBasicDependencyCount();
            for (int i = 0; i < cnt; i++) {
                copy(param, original.getBasicDependency(i));
            }
            return original.accept(this, param);
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
         * Copy the given value.  The value would be given as an argument to an action.
         *
         * @param param the parameter to pass to the visitor methods
         * @param original the value to copy
         * @return the copied value
         */
        Value copy(T param, Value original);

        private List<Value> copy(T param, List<Value> originals) {
            List<Value> copies = new ArrayList<>(originals.size());
            for (Value orig : originals) {
                copies.add(copy(param, orig));
            }
            return copies;
        }

        default Node visit(T param, ArrayElementWrite node) {
            return getBuilder(param).writeArrayValue(copy(param, node.getInstance()), copy(param, node.getIndex()), copy(param, node.getWriteValue()), node.getMode());
        }

        default Node visit(T param, BlockEntry node) {
            return getBuilder(param).getBlockEntry();
        }

        default Node visit(T param, InstanceFieldWrite node) {
            return getBuilder(param).writeInstanceField(copy(param, node.getInstance()), node.getFieldElement(), copy(param, node.getWriteValue()), node.getMode());
        }

        default Node visit(T param, InstanceInvocation node) {
            return getBuilder(param).invokeInstance(node.getKind(), copy(param, node.getInstance()), node.getInvocationTarget(), copy(param, node.getArguments()));
        }

        default Node visit(T param, MonitorEnter node) {
            return getBuilder(param).monitorEnter(copy(param, node.getInstance()));
        }

        default Node visit(T param, MonitorExit node) {
            return getBuilder(param).monitorExit(copy(param, node.getInstance()));
        }

        default Node visit(T param, StaticFieldWrite node) {
            return getBuilder(param).writeStaticField(node.getFieldElement(), copy(param, node.getWriteValue()), node.getMode());
        }

        default Node visit(T param, StaticInvocation node) {
            return getBuilder(param).invokeStatic(node.getInvocationTarget(), copy(param, node.getArguments()));
        }
    }
}
