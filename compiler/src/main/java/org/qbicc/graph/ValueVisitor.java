package org.qbicc.graph;

import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.CompoundLiteral;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.ElementOfLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ValueConvertLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;

/**
 * A visitor over a graph of values.  Values form a directed acyclic graph (DAG).
 */
public interface ValueVisitor<T, R> {
    default R visitUnknown(final T t, Value node) {
        return null;
    }

    default R visit(T t, Add node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, AddressOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, And node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ArrayLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BitCast node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BitCastLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BitReverse node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BlockLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BlockParameter node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BooleanLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ByteArrayLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ByteSwap node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Call node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CallNoSideEffects node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ClassOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Comp node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CountLeadingZeros node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CountTrailingZeros node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Cmp node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CmpAndSwap node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CmpG node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CmpL node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, IsEq node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, IsGe node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, IsGt node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, IsLe node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, IsLt node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, IsNe node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CheckCast node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CompoundLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ConstantLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Convert node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Div node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ElementOfLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Extend node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ExtractElement node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ExtractInstanceField node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ExtractMember node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, FloatLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InsertElement node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InsertMember node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InstanceOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, IntegerLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T param, InterfaceMethodLookup node) {
        return visitUnknown(param, node);
    }

    default R visit(T t, Invoke.ReturnValue node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Load node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Max node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, MemberSelector node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, MethodHandleLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Min node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Mod node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, MultiNewArray node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Multiply node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Neg node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, New node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, NewArray node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, NewReferenceArray node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, NotNull node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, NullLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ObjectLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, OffsetOfField node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Or node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, PointerLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, PopCount node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ReadModifyWrite node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ReferenceTo node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Rol node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Ror node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Select node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Shl node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Shr node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, StackAllocation node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, StringLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Sub node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Truncate node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, TypeLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, UndefinedLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, VaArg node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ValueConvertLiteral node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, VirtualMethodLookup node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Xor node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ZeroInitializerLiteral node) {
        return visitUnknown(t, node);
    }

    interface Delegating<T, R> extends ValueVisitor<T, R> {
        ValueVisitor<T, R> getDelegateValueVisitor();

        default R visitUnknown(final T t, Value node) {
            return node.accept(getDelegateValueVisitor(), t);
        }

        default R visit(T t, Add node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, AddressOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, And node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ArrayLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BitCast node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BitCastLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BitReverse node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BlockLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BlockParameter node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BooleanLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ByteArrayLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ByteSwap node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Call node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, CallNoSideEffects node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, CheckCast node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ClassOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Comp node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, CountLeadingZeros node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, CountTrailingZeros node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Cmp node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, CmpAndSwap node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, CmpG node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, CmpL node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, CompoundLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ConstantLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Convert node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Div node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ElementOfLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Extend node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ExtractElement node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ExtractInstanceField node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ExtractMember node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, FloatLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InsertElement node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InsertMember node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InstanceOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, IntegerLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InterfaceMethodLookup node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Invoke.ReturnValue node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, IsEq node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, IsGe node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, IsGt node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, IsLe node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, IsLt node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, IsNe node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Load node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Max node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, MemberSelector node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, MethodHandleLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Min node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Mod node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, MultiNewArray node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Multiply node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Neg node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, New node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, NewArray node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, NewReferenceArray node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, NotNull node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, NullLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ObjectLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, OffsetOfField node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Or node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, PointerLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, PopCount node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ReadModifyWrite node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ReferenceTo node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Rol node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Ror node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Select node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Shl node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Shr node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, StackAllocation node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, StringLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Sub node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Truncate node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, TypeLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, UndefinedLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, VaArg node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ValueConvertLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, VirtualMethodLookup node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Xor node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ZeroInitializerLiteral node) {
            return getDelegateValueVisitor().visit(t, node);
        }
    }
}
