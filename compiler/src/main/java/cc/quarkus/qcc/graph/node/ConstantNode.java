package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.ParseException;
import cc.quarkus.qcc.graph.type.ByteType;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.DoubleType;
import cc.quarkus.qcc.graph.type.FloatType;
import cc.quarkus.qcc.graph.type.IntType;
import cc.quarkus.qcc.graph.type.LongType;
import cc.quarkus.qcc.graph.type.NullType;
import cc.quarkus.qcc.graph.type.ObjectType;
import cc.quarkus.qcc.graph.type.ShortType;

public class ConstantNode<C, T extends ConcreteType<C>> extends Node<T> {

    public static ConstantNode<Object, NullType> nullConstant(ControlNode<?> control) {
        return new ConstantNode<>(control, NullType.INSTANCE, null);
    }

    public static ConstantNode<Object, ObjectType> stringConstant(ControlNode<?> control, String val) {
        return new ConstantNode<>(control, ObjectType.java.lang.String, val);
    }

    public static ConstantNode<Integer, IntType> intConstant(ControlNode<?> control, int val) {
        return new ConstantNode<>(control, IntType.INSTANCE, val);
    }

    public static ConstantNode<Long, LongType> longConstant(ControlNode<?> control, long val) {
        return new ConstantNode<>(control, LongType.INSTANCE, val);
    }

    public static ConstantNode<Float, FloatType> floatConstant(ControlNode<?> control, float val) {
        return new ConstantNode<>(control, FloatType.INSTANCE, val);
    }

    public static ConstantNode<Double, DoubleType> doubleConstant(ControlNode<?> control, double val) {
        return new ConstantNode<>(control, DoubleType.INSTANCE, val);
    }

    public static ConstantNode<Byte, ByteType> byteConstant(ControlNode<?> control, byte val) {
        return new ConstantNode<>(control, ByteType.INSTANCE, val);
    }

    public static ConstantNode<Short, ShortType> shortConstant(ControlNode<?> control, short val) {
        return new ConstantNode<>(control, ShortType.INSTANCE, val);
    }

    public static ConstantNode<?,?> constant(ControlNode<?> control, Object val) {
        if ( val instanceof String ) {
            return stringConstant(control, (String) val);
        } else if ( val instanceof Byte) {
            return byteConstant(control, (byte) val);
        } else if ( val instanceof Short) {
            return shortConstant(control, (short) val);
        } else if ( val instanceof Integer) {
            return intConstant(control, (int) val);
        } else if ( val instanceof Long) {
            return longConstant(control, (long) val);
        } else if ( val instanceof Float) {
            return floatConstant(control, (float) val);
        } else if ( val instanceof Double) {
            return doubleConstant(control, (double) val);
        }

        throw new ParseException("not a constant: " + val);
    }

    protected ConstantNode(ControlNode<?> control, T outType, C val) {
        super(control, outType);
        this.val = val;
    }

    @Override
    public String label() {
        return "<const> " + this.val;
    }

    private C val;

}
