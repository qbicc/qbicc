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
public interface ValueVisitorLong<T> {
    default long visitUnknown(final T param, Value node) {
        return 0;
    }

    default long visit(T param, Add node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, AddressOf node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, And node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ArrayLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, BitCast node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, BitCastLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, BitReverse node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, BlockLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, BooleanLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ByteArrayLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ByteSwap node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Call node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, CallNoSideEffects node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ClassOf node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Comp node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, CountLeadingZeros node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, CountTrailingZeros node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Cmp node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, CmpAndSwap node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, CmpG node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, CmpL node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, IsEq node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, IsGe node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, IsGt node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, IsLe node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, IsLt node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, IsNe node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, CheckCast node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, CompoundLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ConstantLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Convert node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Div node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ElementOfLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Extend node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ExtractElement node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ExtractInstanceField node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ExtractMember node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, FloatLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, InsertElement node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, InsertMember node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, InstanceOf node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, IntegerLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Invoke.ReturnValue node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Load node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Max node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, MemberSelector node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, MethodHandleLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Min node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Mod node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, MultiNewArray node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Multiply node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Neg node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, New node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, NewArray node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, NewReferenceArray node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, NotNull node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, NullLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ObjectLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, OffsetOfField node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Or node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ParameterValue node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, PhiValue node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, PointerLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, PopCount node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ReadModifyWrite node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ReferenceTo node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Rol node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Ror node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Select node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Shl node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Shr node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, StackAllocation node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, StringLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Sub node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Truncate node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, TypeLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, UndefinedLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, VaArg node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ValueConvertLiteral node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, Xor node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ZeroInitializerLiteral node) {
        return visitUnknown(param, node);
    }

    interface Delegating<T> extends ValueVisitorLong<T> {
        ValueVisitorLong<T> getDelegateValueVisitor();

        default long visitUnknown(final T param, Value node) {
            return node.accept(getDelegateValueVisitor(), param);
        }

        default long visit(T param, Add node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, AddressOf node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, And node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ArrayLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, BitCast node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, BitCastLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, BitReverse node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, BlockLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, BooleanLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ByteArrayLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ByteSwap node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Call node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, CallNoSideEffects node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, CheckCast node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ClassOf node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Comp node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, CountLeadingZeros node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, CountTrailingZeros node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Cmp node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, CmpAndSwap node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, CmpG node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, CmpL node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, CompoundLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ConstantLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Convert node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Div node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ElementOfLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Extend node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ExtractElement node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ExtractInstanceField node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ExtractMember node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, FloatLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, InsertElement node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, InsertMember node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, InstanceOf node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, IntegerLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Invoke.ReturnValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, IsEq node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, IsGe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, IsGt node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, IsLe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, IsLt node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, IsNe node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Load node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Max node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, MemberSelector node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, MethodHandleLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Min node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Mod node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, MultiNewArray node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Multiply node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Neg node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, New node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, NewArray node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, NewReferenceArray node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, NotNull node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, NullLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ObjectLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, OffsetOfField node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Or node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ParameterValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, PhiValue node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, PointerLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, PopCount node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ReadModifyWrite node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ReferenceTo node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Rol node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Ror node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Select node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Shl node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Shr node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, StackAllocation node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, StringLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Sub node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Truncate node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, TypeLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, UndefinedLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, VaArg node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ValueConvertLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, Xor node) {
            return getDelegateValueVisitor().visit(param, node);
        }

        default long visit(T param, ZeroInitializerLiteral node) {
            return getDelegateValueVisitor().visit(param, node);
        }
    }
}
