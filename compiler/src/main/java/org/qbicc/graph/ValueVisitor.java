package org.qbicc.graph;

import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralVisitor;

/**
 * A visitor over a graph of values.  Values form a directed acyclic graph (DAG).
 */
public interface ValueVisitor<T, R> extends LiteralVisitor<T, R> {
    default R visitUnknown(final T t, Value node) {
        return null;
    }

    default R visitAny(T t, Literal literal) {
        return visitUnknown(t, literal);
    }

    default R visit(T t, Add node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, And node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Auto node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BitCast node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BitReverse node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, BlockParameter node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ByteOffsetPointer node) {
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

    default R visit(T t, CurrentThread node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, DecodeReference node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Div node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ElementOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, EncodeReference node) {
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

    default R visit(T t, FpToInt node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InsertElement node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InsertMember node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InstanceFieldOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InstanceOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InterfaceMethodLookup node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, IntToFp node) {
        return visitUnknown(t, node);
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

    default R visit(T t, MemberOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, MemberOfUnion node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Dereference node) {
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

    default R visit(T t, OffsetOfField node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, OffsetPointer node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Or node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, PointerDifference node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, PopCount node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ReadModifyWrite node) {
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

    default R visit(T t, Sub node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ThreadBound node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Truncate node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, VaArg node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, VirtualMethodLookup node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Xor node) {
        return visitUnknown(t, node);
    }

    interface Delegating<T, R> extends ValueVisitor<T, R>, LiteralVisitor.Delegating<T, R> {
        ValueVisitor<T, R> getDelegateValueVisitor();

        default LiteralVisitor<T, R> getDelegateLiteralVisitor() {
            return getDelegateValueVisitor();
        }

        default R visitUnknown(final T t, Value node) {
            return node.accept(getDelegateValueVisitor(), t);
        }

        default R visit(T t, Add node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, And node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Auto node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BitCast node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BitReverse node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, BlockParameter node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ByteOffsetPointer node) {
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

        default R visit(T t, CurrentThread node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, DecodeReference node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Div node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ElementOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, EncodeReference node) {
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

        default R visit(T t, FpToInt node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InsertElement node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InsertMember node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InstanceFieldOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InstanceOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, InterfaceMethodLookup node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, IntToFp node) {
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

        default R visit(T t, MemberOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, MemberOfUnion node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Dereference node) {
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

        default R visit(T t, OffsetOfField node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, OffsetPointer node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Or node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, PointerDifference node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, PopCount node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ReadModifyWrite node) {
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

        default R visit(T t, Sub node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, ThreadBound node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Truncate node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, VaArg node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, VirtualMethodLookup node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        default R visit(T t, Xor node) {
            return getDelegateValueVisitor().visit(t, node);
        }
    }
}
