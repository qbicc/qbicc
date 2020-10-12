package cc.quarkus.qcc.graph;

import java.nio.charset.StandardCharsets;

import cc.quarkus.qcc.constraint.Constraint;

public interface Value extends Node {
    ConstantValue NULL = new ConstantValueEmpty(Type.NULL_TYPE);

    Constraint getConstraint();

    Type getType();

    void setConstraint(Constraint constraint);

    <T, R> R accept(ValueVisitor<T, R> visitor, T param);

    Value[] NO_VALUES = new Value[0];

    ConstantValue FALSE = ((BooleanTypeImpl)Type.BOOL).false_;
    ConstantValue TRUE = ((BooleanTypeImpl)Type.BOOL).true_;

    ConstantValue ICONST_0 = const_(0);
    ConstantValue LCONST_0 = Value.const_(0);

    static ConstantValue const_(int operand) {
        // todo: cache
        return new ConstantValue32(operand, Type.S32);
    }

    static ConstantValue const_(int operand, Type type) {
        if (type == Type.BOOL) {
            return operand == 0 ? Value.FALSE : Value.TRUE;
        } else if (operand == 0 && type == Type.S32) {
            return ICONST_0;
        }
        // todo: cache
        return new ConstantValue32(operand, type);
    }

    static ConstantValue const_(long operand) {
        // todo: cache
        return new ConstantValue64(operand, Type.S64);
    }

    static ConstantValue const_(long operand, Type type) {
        // todo: cache
        return new ConstantValue64(operand, type);
    }

    static ConstantValue const_(float floatValue) {
        // todo: cache
        return new ConstantValue32(Float.floatToIntBits(floatValue), Type.F32);
    }

    static ConstantValue const_(double doubleValue) {
        // todo: cache
        return new ConstantValue64(Double.doubleToLongBits(doubleValue), Type.F64);
    }

    static ConstantValue const_(String str) {
        // todo: cache
        return const_(str.getBytes(StandardCharsets.UTF_8), Type.STRING);
    }

    static ConstantValue const_(byte[] bytes, Type type) {
        // todo: cache
        return new ConstantValueBig(bytes, type);
    }

    static ConstantValue const_(ClassType classType) {
        // todo: cache
        return new ClassLiteralValue(classType);
    }

    static ConstantValue const_(BlockLabel blockLabel) {
        return new BlockLiteralValue(blockLabel);
    }
}
