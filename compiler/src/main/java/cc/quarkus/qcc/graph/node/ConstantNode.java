package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.ParseException;
import cc.quarkus.qcc.graph.type.ByteType;
import cc.quarkus.qcc.graph.type.ByteValue;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.DoubleType;
import cc.quarkus.qcc.graph.type.DoubleValue;
import cc.quarkus.qcc.graph.type.FloatType;
import cc.quarkus.qcc.graph.type.FloatValue;
import cc.quarkus.qcc.graph.type.IntType;
import cc.quarkus.qcc.graph.type.IntValue;
import cc.quarkus.qcc.graph.type.LongType;
import cc.quarkus.qcc.graph.type.LongValue;
import cc.quarkus.qcc.graph.type.NullType;
import cc.quarkus.qcc.graph.type.NullValue;
import cc.quarkus.qcc.graph.type.ObjectType;
import cc.quarkus.qcc.graph.type.ObjectValue;
import cc.quarkus.qcc.graph.type.ShortType;
import cc.quarkus.qcc.graph.type.ShortValue;
import cc.quarkus.qcc.graph.type.Value;

public class ConstantNode<C extends Value<T>, T extends ConcreteType<C>> extends Node<T> {

    public static ConstantNode<NullValue, NullType> nullConstant(ControlNode<?> control) {
        return new ConstantNode<>(control, NullValue.NULL);
    }

    public static ConstantNode<ObjectValue, ObjectType> stringConstant(ControlNode<?> control, String val) {
        return new ConstantNode<>(control, ObjectType.java.lang.String.newInstance(val));
    }

    public static ConstantNode<IntValue, IntType> intConstant(ControlNode<?> control, int val) {
        return new ConstantNode<>(control, new IntValue(val));
    }

    public static ConstantNode<LongValue, LongType> longConstant(ControlNode<?> control, long val) {
        return new ConstantNode<>(control, new LongValue(val));
    }

    public static ConstantNode<FloatValue, FloatType> floatConstant(ControlNode<?> control, float val) {
        return new ConstantNode<>(control, new FloatValue(val));
    }

    public static ConstantNode<DoubleValue, DoubleType> doubleConstant(ControlNode<?> control, double val) {
        return new ConstantNode<>(control, new DoubleValue(val));
    }

    public static ConstantNode<ByteValue, ByteType> byteConstant(ControlNode<?> control, byte val) {
        return new ConstantNode<>(control, new ByteValue(val));
    }

    public static ConstantNode<ShortValue, ShortType> shortConstant(ControlNode<?> control, short val) {
        return new ConstantNode<>(control, new ShortValue(val));
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

    protected ConstantNode(ControlNode<?> control, C val) {
        super(control, val.getType());
        this.val = val;
    }

    @Override
    public String label() {
        return "<const> " + this.val;
    }

    private C val;

}
