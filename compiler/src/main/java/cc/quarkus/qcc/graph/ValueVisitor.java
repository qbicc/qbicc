package cc.quarkus.qcc.graph;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.literal.ArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.InterfaceTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.literal.ObjectLiteral;
import cc.quarkus.qcc.graph.literal.ReferenceArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.StringLiteral;
import cc.quarkus.qcc.graph.literal.ValueArrayTypeIdLiteral;

/**
 * A visitor over a graph of values.  Values form a directed acyclic graph (DAG).
 */
public interface ValueVisitor<T, R> {
    default R visitUnknown(final T param, Value node) {
        return null;
    }

    default R visit(T param, Add node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, And node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ArrayElementRead node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ArrayLength node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, BitCast node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, BlockLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, BooleanLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Catch node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ClassTypeIdLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpEq node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpGe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpGt node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpLe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpLt node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpNe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ConstructorInvocation node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Convert node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Div node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Extend node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, FloatLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceFieldRead node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceInvocationValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, IntegerLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InterfaceTypeIdLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Mod node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Multiply node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Narrow node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Neg node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, New node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, NewArray node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, NullLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ObjectLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Or node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ParameterValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, PhiValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ReferenceArrayTypeIdLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Rol node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Ror node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Select node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Shl node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Shr node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticFieldRead node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticInvocationValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StringLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Sub node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ThisValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Truncate node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, TypeIdOf node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ValueArrayTypeIdLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Xor node) {
        return visitUnknown(param, node);
    }

    interface Delegating<T, R> extends ValueVisitor<T, R> {
        ValueVisitor<T, R> getDelegateValueVisitor();

        default R visitUnknown(final T param, Value node) {
            return node.accept(getDelegateValueVisitor(), param);
        }

        default R visit(T param, Add node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, And node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ArrayElementRead node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ArrayLength node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, BitCast node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, BlockLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, BooleanLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Catch node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ClassTypeIdLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, CmpEq node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, CmpGe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, CmpGt node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, CmpLe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, CmpLt node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, CmpNe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ConstructorInvocation node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Convert node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Div node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Extend node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, FloatLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, InstanceFieldRead node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, InstanceInvocationValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, IntegerLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, InterfaceTypeIdLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Mod node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Multiply node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Narrow node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Neg node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, New node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, NewArray node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, NullLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ObjectLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Or node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ParameterValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, PhiValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ReferenceArrayTypeIdLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Rol node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Ror node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Select node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Shl node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Shr node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, StaticFieldRead node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, StaticInvocationValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, StringLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Sub node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ThisValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Truncate node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, TypeIdOf node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ValueArrayTypeIdLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Xor node) {
            return getDelegateValueVisitor().visit(param, node);
        }
    }

    /**
     * A value visitor base interface which recursively copies the given values.  Literals are not copied, nor are parameter
     * values. Also, {@code phi} nodes are not populated (this is the responsibility of the subclass), though each
     * encountered {@code phi} node is registered to the {@link #registerPhi(Object, PhiValue, PhiValue) registerPhi()} method.
     * <p>
     * To transform certain value types, override the specific {@code visit()} method for that type.
     *
     * @param <T> the parameter type
     * @deprecated Replace with {@link Node.Copier}.
     */
    @Deprecated
    interface Copying<T> extends ValueVisitor<T, Value> {

        /**
         * Get the block builder to use to construct copied nodes.  This method should return the same builder instance
         * on every call with the same parameter.
         *
         * @param param the parameter which was passed in to the {@link #copy} method
         * @return the builder
         */
        BasicBlockBuilder getBuilder(T param);

        /**
         * Copy the label for the given block.
         *
         * @param param the parameter to pass to the visitor methods
         * @param block the block to copy
         * @return the copied label
         */
        BlockLabel copy(T param, BasicBlock block);

        /**
         * Get the visitor entry point to use for copying a value.  The visitor should ultimately delegate back
         * to this visitor.
         *
         * @return the copying visitor
         */
        default ValueVisitor<T, Value> getCopyingValueVisitor() {
            return this;
        }

        /**
         * Entry point to copy the given value.  Subclasses may introduce caching or mapping at this point.  The
         * default implementation always makes a copy and returns it.
         *
         * @param param the parameter to pass to the visitor methods
         * @param original the value to copy
         * @return the copy
         */
        default Value copy(T param, Value original) {
            int cnt = original.getBasicDependencyCount();
            for (int i = 0; i < cnt; i++) {
                copy(param, original.getBasicDependency(i));
            }
            BasicBlockBuilder builder = getBuilder(param);
            int oldLine = builder.setLineNumber(original.getSourceLine());
            int oldBci = builder.setBytecodeIndex(original.getBytecodeIndex());
            try {
                return original.accept(getCopyingValueVisitor(), param);
            } finally {
                builder.setBytecodeIndex(oldBci);
                builder.setLineNumber(oldLine);
            }
        }

        /**
         * Make a copy of a dependency node.
         *
         * @param param the parameter to pass to the visitor methods
         * @param original the node to copy
         * @return the copied node
         */
        Node copy(T param, Node original);

        private List<Value> copy(T param, List<Value> list) {
            ArrayList<Value> toList = new ArrayList<>(list.size());
            for (Value value : list) {
                toList.add(copy(param, value));
            }
            return toList;
        }

        /**
         * Register that a {@code phi} node was copied.  The visitor should take care to iterate the incoming
         * values for the {@code phi} and ensure they are mapped and copied after the visitor returns.
         *
         * @param param the visitor parameter
         * @param originalPhi the {@code phi} node that was copied
         * @param newPhi the {@code phi} node that was created
         */
        void registerPhi(T param, PhiValue originalPhi, PhiValue newPhi);

        default Value visit(final T param, final Add node) {
            return getBuilder(param).add(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final And node) {
            return getBuilder(param).and(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final ArrayElementRead node) {
            return getBuilder(param).readArrayValue(copy(param, node.getInstance()), copy(param, node.getIndex()), node.getMode());
        }

        default Value visit(final T param, final ArrayLength node) {
            return getBuilder(param).arrayLength(copy(param, node.getInstance()));
        }

        default Value visit(final T param, final BitCast node) {
            return getBuilder(param).bitCast(copy(param, node.getInput()), node.getType());
        }

        default Value visit(final T param, final BlockLiteral node) {
            return node;
        }

        default Value visit(final T param, final BooleanLiteral node) {
            return node;
        }

        default Value visit(final T param, final Catch node) {
            return getBuilder(param).catch_(node.getType().getUpperBound());
        }

        default Value visit(final T param, final ClassTypeIdLiteral node) {
            return node;
        }

        default Value visit(final T param, final CmpEq node) {
            return getBuilder(param).cmpEq(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final CmpGe node) {
            return getBuilder(param).cmpGe(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final CmpGt node) {
            return getBuilder(param).cmpGt(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final CmpLe node) {
            return getBuilder(param).cmpLe(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final CmpLt node) {
            return getBuilder(param).cmpLt(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final CmpNe node) {
            return getBuilder(param).cmpNe(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final ConstructorInvocation node) {
            return getBuilder(param).invokeConstructor(copy(param, node.getInstance()), node.getInvocationTarget(), copy(param, node.getArguments()));
        }

        default Value visit(final T param, final Convert node) {
            return getBuilder(param).valueConvert(copy(param, node.getInput()), node.getType());
        }

        default Value visit(final T param, final Div node) {
            return getBuilder(param).divide(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final Extend node) {
            return getBuilder(param).extend(copy(param, node.getInput()), node.getType());
        }

        default Value visit(final T param, final FloatLiteral node) {
            return node;
        }

        default Value visit(final T param, final InstanceFieldRead node) {
            return getBuilder(param).readInstanceField(copy(param, node.getInstance()), node.getFieldElement(), node.getMode());
        }

        default Value visit(final T param, final InstanceInvocationValue node) {
            return getBuilder(param).invokeValueInstance(copy(param, node.getInstance()), node.getKind(), node.getInvocationTarget(), copy(param, node.getArguments()));
        }

        default Value visit(final T param, final IntegerLiteral node) {
            return node;
        }

        default Value visit(final T param, final InterfaceTypeIdLiteral node) {
            return node;
        }

        default Value visit(final T param, final Mod node) {
            return getBuilder(param).remainder(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final Multiply node) {
            return getBuilder(param).multiply(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final Narrow node) {
            return getBuilder(param).narrow(copy(param, node.getInput()), node.getType().getUpperBound());
        }

        default Value visit(final T param, final Neg node) {
            return getBuilder(param).negate(copy(param, node.getInput()));
        }

        default Value visit(final T param, final New node) {
            return getBuilder(param).new_(node.getInstanceTypeId());
        }

        default Value visit(final T param, final NewArray node) {
            return getBuilder(param).newArray((ArrayTypeIdLiteral)node.getType().getUpperBound(), copy(param, node.getSize()));
        }

        default Value visit(final T param, final NullLiteral node) {
            return node;
        }

        default Value visit(final T param, final ObjectLiteral node) {
            return node;
        }

        default Value visit(final T param, final Or node) {
            return getBuilder(param).or(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final ParameterValue node) {
            return node;
        }

        default Value visit(final T param, final PhiValue node) {
            return getBuilder(param).phi(node.getType(), copy(param, node.getPinnedBlock()));
        }

        default Value visit(final T param, final ReferenceArrayTypeIdLiteral node) {
            return node;
        }

        default Value visit(final T param, final Rol node) {
            return getBuilder(param).rol(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final Ror node) {
            return getBuilder(param).ror(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final Select node) {
            return getBuilder(param).select(copy(param, node.getCondition()), copy(param, node.getTrueValue()), copy(param, node.getFalseValue()));
        }

        default Value visit(final T param, final Shl node) {
            return getBuilder(param).shl(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final Shr node) {
            return getBuilder(param).shr(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final StaticFieldRead node) {
            return getBuilder(param).readStaticField(node.getFieldElement(), node.getMode());
        }

        default Value visit(final T param, final StaticInvocationValue node) {
            return getBuilder(param).invokeValueStatic(node.getInvocationTarget(), copy(param, node.getArguments()));
        }

        default Value visit(final T param, final StringLiteral node) {
            return node;
        }

        default Value visit(final T param, final Sub node) {
            return getBuilder(param).sub(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }

        default Value visit(final T param, final ThisValue node) {
            return node;
        }

        default Value visit(final T param, final Truncate node) {
            return getBuilder(param).truncate(copy(param, node.getInput()), node.getType());
        }

        default Value visit(final T param, final TypeIdOf node) {
            return getBuilder(param).typeIdOf(copy(param, node.getInstance()));
        }

        default Value visit(final T param, final ValueArrayTypeIdLiteral node) {
            return node;
        }

        default Value visit(final T param, final Xor node) {
            return getBuilder(param).xor(copy(param, node.getLeftInput()), copy(param, node.getRightInput()));
        }
    }
}
