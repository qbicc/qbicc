package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.ParseException;
import cc.quarkus.qcc.graph.type.IntrinsicObjectReference;
import cc.quarkus.qcc.graph.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.Sentinel;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ConstantNode<V> extends AbstractNode<V> {

    public static ConstantNode<Sentinel.Void> voidConstant(ControlNode<?> control) {
        return new ConstantNode<>(control, Sentinel.Void.VOID, TypeDescriptor.VOID);
    }

    public static ConstantNode<ObjectReference> nullConstant(ControlNode<?> control) {
        ObjectReference constantVal = IntrinsicObjectReference.newNull();
        return new ConstantNode<>(control, constantVal, constantVal.getTypeDescriptor());
    }

    public static ConstantNode<ObjectReference> stringConstant(ControlNode<?> control, String val) {
        ObjectReference constantVal = IntrinsicObjectReference.newString(val);
        return new ConstantNode<>(control, constantVal, constantVal.getTypeDescriptor());
    }

    public static ConstantNode<Integer> intConstant(ControlNode<?> control, int val) {
        return new ConstantNode<>(control, val, TypeDescriptor.INT);
    }

    public static ConstantNode<Long> longConstant(ControlNode<?> control, long val) {
        return new ConstantNode<>(control, val, TypeDescriptor.LONG);
    }

    public static ConstantNode<Float> floatConstant(ControlNode<?> control, float val) {
        return new ConstantNode<>(control, val, TypeDescriptor.FLOAT);
    }

    public static ConstantNode<Double> doubleConstant(ControlNode<?> control, double val) {
        return new ConstantNode<>(control, val, TypeDescriptor.DOUBLE);
    }

    public static ConstantNode<Byte> byteConstant(ControlNode<?> control, byte val) {
        return new ConstantNode<>(control, val, TypeDescriptor.BYTE);
    }

    public static ConstantNode<Short> shortConstant(ControlNode<?> control, short val) {
        return new ConstantNode<>(control, val, TypeDescriptor.SHORT);
    }

    @SuppressWarnings("unchecked")
    public static <V> ConstantNode<V> constant(ControlNode<?> control, V val) {
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

    protected ConstantNode(ControlNode<?> control, V val, TypeDescriptor<V> type) {
        super(control, type);
        this.val = val;
    }

    @Override
    public V getValue(Context context) {
        return this.val;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<const> " + getTypeDescriptor().label() + "=" + this.val;
    }

    private V val;

}
