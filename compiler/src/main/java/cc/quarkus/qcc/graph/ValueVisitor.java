package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.literal.ArrayLiteral;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.CompoundLiteral;
import cc.quarkus.qcc.graph.literal.CurrentThreadLiteral;
import cc.quarkus.qcc.graph.literal.DefinedConstantLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.MethodDescriptorLiteral;
import cc.quarkus.qcc.graph.literal.MethodHandleLiteral;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.literal.ObjectLiteral;
import cc.quarkus.qcc.graph.literal.StringLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.TypeLiteral;
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;

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

    default R visit(T param, ArrayLiteral node) {
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

    default R visit(T param, Clone node) {
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

    default R visit(T param, CompoundLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ConstructorInvocation node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Convert node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CurrentThreadLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, DefinedConstantLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Div node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, DynamicInvocationValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Extend node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, FloatLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, FunctionCall node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceFieldRead node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceInvocationValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceOf node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, IntegerLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MemberPointer node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MethodDescriptorLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MethodHandleLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Mod node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MultiNewArray node) {
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

    default R visit(T param, PointerLoad node) {
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

    default R visit(T param, StackAllocation node) {
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

    default R visit(T param, SymbolLiteral node) {
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

    default R visit(T param, TypeLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, UndefinedLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Xor node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ZeroInitializerLiteral node) {
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

        default R visit(T param, ArrayLiteral node) {
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

        default R visit(T param, Clone node) {
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

        default R visit(T param, CompoundLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ConstructorInvocation node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Convert node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, CurrentThreadLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, DefinedConstantLiteral node) {
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

        default R visit(T param, FunctionCall node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, InstanceFieldRead node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, InstanceInvocationValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, InstanceOf node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, IntegerLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, MemberPointer node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, MethodDescriptorLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, MethodHandleLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Mod node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, MultiNewArray node) {
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

        default R visit(T param, PointerLoad node) {
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

        default R visit(T param, StackAllocation node) {
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

        default R visit(T param, SymbolLiteral node) {
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

        default R visit(T param, TypeLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, UndefinedLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Xor node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ZeroInitializerLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }
    }
}
