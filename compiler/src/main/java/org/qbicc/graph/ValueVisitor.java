package org.qbicc.graph;

import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.CompoundLiteral;
import org.qbicc.graph.literal.DefinedConstantLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.MethodDescriptorLiteral;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ValueConvertLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;

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

    default R visit(T param, AddressOf node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, And node) {
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

    default R visit(T param, BitCastLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, BlockLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, BooleanLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ByteArrayLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ClassOf node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Clone node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Cmp node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpAndSwap node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpG node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpL node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, IsEq node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, IsGe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, IsGt node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, IsLe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, IsLt node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, IsNe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CheckCast node) {
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

    default R visit(T param, CurrentThreadRead node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, DefinedConstantLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Div node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Extend node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ExtractElement node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ExtractInstanceField node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ExtractMember node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, FloatLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, FunctionCall node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndAdd node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndBitwiseAnd node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndBitwiseNand node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndBitwiseOr node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndBitwiseXor node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndSet node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndSetMax node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndSetMin node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GetAndSub node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InsertElement node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InsertMember node) {
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

    default R visit(T param, Load node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Max node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MethodDescriptorLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MethodHandleLiteral node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Min node) {
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

    default R visit(T param, Neg node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, New node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, NewArray node) {
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

    default R visit(T param, ValueConvertLiteral node) {
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

        default R visit(T param, AddressOf node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, And node) {
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

        default R visit(T param, ByteArrayLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, CheckCast node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ClassOf node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Clone node) {
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

        default R visit(T param, DefinedConstantLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Div node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Extend node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ExtractElement node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ExtractInstanceField node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, ExtractMember node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, FloatLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, FunctionCall node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndAdd node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndBitwiseAnd node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndBitwiseNand node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndBitwiseOr node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndBitwiseXor node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndSet node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndSetMax node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndSetMin node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, GetAndSub node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, InsertElement node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, InsertMember node) {
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

        default R visit(T param, IsEq node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, IsGe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, IsGt node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, IsLe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, IsLt node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, IsNe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Load node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Max node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, MethodDescriptorLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, MethodHandleLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, Min node) {
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

        default R visit(T param, Neg node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, New node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default R visit(T param, NewArray node) {
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

        default R visit(T param, ValueConvertLiteral node) {
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
