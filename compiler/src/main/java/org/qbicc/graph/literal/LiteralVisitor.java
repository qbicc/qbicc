package org.qbicc.graph.literal;

/**
 * A visitor over every possible kind of literal.
 */
public interface LiteralVisitor<T, R> {
    default R visitAny(T t, Literal literal) {
        return null;
    }

    default R visit(T t, ArrayLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, AsmLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, BitCastLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, BlockLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, BooleanLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, ByteArrayLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, StructLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, ConstantLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, ConstructorLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, ElementOfLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, EncodeReferenceLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, FloatLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, FunctionLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, GlobalVariableLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, InitializerLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, InstanceMethodLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, IntegerLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, MemberOfLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, MethodHandleLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, NullLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, ObjectLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, OffsetFromLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, ProgramObjectLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, ShortArrayLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, StaticFieldLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, StaticMethodLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, StringLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, TypeIdLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, UndefinedLiteral literal) {
        return visitAny(t, literal);
    }

    default R visit(T t, ZeroInitializerLiteral literal) {
        return visitAny(t, literal);
    }

    interface Delegating<T, R> extends LiteralVisitor<T, R> {
        LiteralVisitor<T, R> getDelegateLiteralVisitor();

        default R visit(T t, ArrayLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, AsmLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, BitCastLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, BlockLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, BooleanLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, ByteArrayLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, StructLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, ConstantLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, ConstructorLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, ElementOfLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, EncodeReferenceLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, FloatLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, FunctionLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, GlobalVariableLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, InitializerLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, InstanceMethodLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, IntegerLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, MemberOfLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, MethodHandleLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, NullLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, ObjectLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, OffsetFromLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, ProgramObjectLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, ShortArrayLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, StaticFieldLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, StaticMethodLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, StringLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, TypeIdLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, UndefinedLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }

        default R visit(T t, ZeroInitializerLiteral literal) {
            return getDelegateLiteralVisitor().visit(t, literal);
        }
    }
}
