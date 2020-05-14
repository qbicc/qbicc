package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.ParseException;
import cc.quarkus.qcc.graph.type.IntrinsicObjectReference;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QDouble;
import cc.quarkus.qcc.type.QFloat;
import cc.quarkus.qcc.type.QInt16;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.QInt8;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.QVoid;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class ConstantNode<V extends QType> extends AbstractNode<V> {

    public static ConstantNode<QVoid> voidConstant(ControlNode<?> control) {
        return new ConstantNode<>(control.getGraph(), control, QVoid.VOID, TypeDescriptor.VOID);
    }

    public static ConstantNode<ObjectReference> nullConstant(ControlNode<?> control) {
        ObjectReference constantVal = IntrinsicObjectReference.newNull();
        return new ConstantNode<>(control.getGraph(), control, constantVal, constantVal.getTypeDescriptor());
    }

    public static ConstantNode<ObjectReference> stringConstant(ControlNode<?> control, String val) {
        ObjectReference constantVal = IntrinsicObjectReference.newString(val);
        return new ConstantNode<>(control.getGraph(), control, constantVal, constantVal.getTypeDescriptor());
    }

    public static ConstantNode<QInt32> intConstant(ControlNode<?> control, int val) {
        return new ConstantNode<>(control.getGraph(), control, QInt32.of(val), TypeDescriptor.INT32);
    }

    public static ConstantNode<QInt64> longConstant(ControlNode<?> control, long val) {
        return new ConstantNode<>(control.getGraph(), control, QInt64.of(val), TypeDescriptor.INT64);
    }

    public static ConstantNode<QFloat> floatConstant(ControlNode<?> control, float val) {
        return new ConstantNode<>(control.getGraph(), control, QFloat.of(val), TypeDescriptor.FLOAT);
    }

    public static ConstantNode<QDouble> doubleConstant(ControlNode<?> control, double val) {
        return new ConstantNode<>(control.getGraph(), control, QDouble.of(val), TypeDescriptor.DOUBLE);
    }

    public static ConstantNode<QInt8> byteConstant(ControlNode<?> control, byte val) {
        return new ConstantNode<>(control.getGraph(), control, QInt8.of( val) , TypeDescriptor.INT8);
    }

    public static ConstantNode<QInt16> shortConstant(ControlNode<?> control, short val) {
        return new ConstantNode<>(control.getGraph(), control, QInt16.of(val), TypeDescriptor.INT16);
    }

    @SuppressWarnings("unchecked")
    public static <V extends QType> ConstantNode<V> constant(ControlNode<?> control, Object val) {
        if ( val instanceof String ) {
            return (ConstantNode<V>) stringConstant(control, (String) val);
        } else if ( val instanceof Byte) {
            return (ConstantNode<V>) byteConstant(control, (byte) val);
        } else if ( val instanceof Short) {
            return (ConstantNode<V>) shortConstant(control, (short) val);
        } else if ( val instanceof Integer) {
            return (ConstantNode<V>) intConstant(control, (int) val);
        } else if ( val instanceof Long) {
            return (ConstantNode<V>) longConstant(control, (long) val);
        } else if ( val instanceof Float) {
            return (ConstantNode<V>) floatConstant(control, (float) val);
        } else if ( val instanceof Double) {
            return (ConstantNode<V>) doubleConstant(control, (double) val);
        }

        throw new ParseException("not a constant: " + val);
    }

    protected ConstantNode(Graph<?> graph, ControlNode<?> control, V val, TypeDescriptor<V> type) {
        super(graph, control, type);
        this.val = val;
    }

    @Override
    public V getValue(Context context) {
        return this.val;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of(getControl());
    }

    @Override
    public String label() {
        return "<const> " + getTypeDescriptor().label() + "=" + this.val;
    }

    @Override
    public String toString() {
        return label();
    }

    private V val;

}
