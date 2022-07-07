package org.qbicc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmString;

import static org.qbicc.graph.atomic.AccessModes.SingleAcquire;
import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

final class VmStringImpl extends VmObjectImpl implements VmString {
    private static final VarHandle contentHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "content", VarHandle.class, VmStringImpl.class, String.class);

    private volatile String content;

    VmStringImpl(VmImpl vm, VmClassImpl clazz, String content) {
        super(clazz);
        this.content = content;
        // simulate special constructor to initialize class
        boolean latin1 = true;
        int length = content.length();
        for (int i = 0; i < length; i ++) {
            if (content.charAt(i) > 255) {
                latin1 = false;
                break;
            }
        }
        byte[] bytes;
        if (latin1) {
            bytes = content.getBytes(StandardCharsets.ISO_8859_1);
        } else {
            bytes = content.getBytes(StandardCharsets.UTF_16LE);
        }
        VmArray byteArray = vm.newByteArray(bytes);
        Memory memory = getMemory();
        memory.store8(vm.stringCoderOffset, latin1 ? 0 : 1, SinglePlain);
        memory.storeRef(vm.stringValueOffset, byteArray, SinglePlain);
    }

    VmStringImpl(VmClassImpl clazz) {
        super(clazz);
    }

    VmStringImpl(final VmStringImpl original) {
        super(original);
        content = original.content;
    }

    @Override
    public String getContent() {
        String content = this.content;
        if (content != null) {
            return content;
        }
        VmImpl vm = getVmClass().getVm();
        Memory memory = getMemory();
        int coder = memory.load8(vm.stringCoderOffset, SingleAcquire);
        VmByteArrayImpl array = (VmByteArrayImpl) memory.loadRef(vm.stringValueOffset, SingleAcquire);
        // use an intermediate `char` array to avoid invalid conversion problems
        byte[] byteArray = array.getArray();
        int shifted = byteArray.length >> coder;
        char[] charArray = new char[shifted];
        if (coder == 0) {
            for (int i = 0; i < byteArray.length; i++) {
                charArray[i] = (char) (byteArray[i] & 0xff);
            }
        } else {
            int byteIdx;
            for (int i = 0; i < shifted; i ++) {
                byteIdx = i << 1;
                charArray[i] = (char) (byteArray[byteIdx] & 0xff | (byteArray[byteIdx + 1] & 0xff) << 8);
            }
        }
        String newContent = new String(charArray);
        for (;;) {
            if (contentHandle.compareAndSet(this, null, newContent)) {
                return newContent;
            }
            content = this.content;
            if (content != null) {
                return content;
            }
        }
    }

    @Override
    public String toString() {
        return getContent();
    }

    @Override
    StringBuilder toString(StringBuilder target) {
        return target.append(getContent());
    }

    @Override
    public boolean contentEquals(String string) {
        return getContent().equals(string);
    }

    @Override
    protected VmStringImpl clone() {
        return new VmStringImpl(this);
    }
}
