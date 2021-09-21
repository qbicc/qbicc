package org.qbicc.interpreter;

import java.util.List;

import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
public interface VmInvokable {
    default void invokeVoid(VmThread thread, VmObject target, List<Object> args) {
        invokeAny(thread, target, args);
    }

    default byte invokeByte(VmThread thread, VmObject target, List<Object> args) {
        return ((Byte) invokeAny(thread, target, args)).byteValue();
    }

    default short invokeShort(VmThread thread, VmObject target, List<Object> args) {
        return ((Short) invokeAny(thread, target, args)).shortValue();
    }

    default char invokeChar(VmThread thread, VmObject target, List<Object> args) {
        return ((Character) invokeAny(thread, target, args)).charValue();
    }

    default int invokeInt(VmThread thread, VmObject target, List<Object> args) {
        return ((Integer) invokeAny(thread, target, args)).intValue();
    }

    default long invokeLong(VmThread thread, VmObject target, List<Object> args) {
        return ((Long) invokeAny(thread, target, args)).longValue();
    }

    default float invokeFloat(VmThread thread, VmObject target, List<Object> args) {
        return ((Float) invokeAny(thread, target, args)).floatValue();
    }

    default double invokeDouble(VmThread thread, VmObject target, List<Object> args) {
        return ((Double) invokeAny(thread, target, args)).doubleValue();
    }

    default VmObject invoke(VmThread thread, VmObject target, List<Object> args) {
        return (VmObject) invokeAny(thread, target, args);
    }

    Object invokeAny(VmThread thread, VmObject target, List<Object> args);
}
