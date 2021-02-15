package org.qbicc.interpreter.impl;

import java.util.List;

import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
interface VmInvokable {
    void invokeVoid(VmThread thread, VmObject target, List<Object> args);
    byte invokeByte(VmThread thread, VmObject target, List<Object> args);
    short invokeShort(VmThread thread, VmObject target, List<Object> args);
    char invokeChar(VmThread thread, VmObject target, List<Object> args);
    int invokeInt(VmThread thread, VmObject target, List<Object> args);
    long invokeLong(VmThread thread, VmObject target, List<Object> args);
    float invokeFloat(VmThread thread, VmObject target, List<Object> args);
    double invokeDouble(VmThread thread, VmObject target, List<Object> args);
    VmObject invoke(VmThread thread, VmObject target, List<Object> args);
    Object invokeAny(VmThread thread, VmObject target, List<Object> args);
}
