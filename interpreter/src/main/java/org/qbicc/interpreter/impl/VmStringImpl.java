package org.qbicc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmString;

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
            if (vm.getCompilationContext().getTypeSystem().getEndianness() == ByteOrder.BIG_ENDIAN) {
                bytes = content.getBytes(StandardCharsets.UTF_16BE);
            } else {
                bytes = content.getBytes(StandardCharsets.UTF_16LE);
            }
        }
        VmArray byteArray = vm.allocateArray(bytes);
        MemoryImpl memory = getMemory();
        memory.store8(vm.stringCoderOffset, latin1 ? 0 : 1, MemoryAtomicityMode.UNORDERED);
        memory.storeRef(vm.stringValueOffset, byteArray, MemoryAtomicityMode.UNORDERED);
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
        VmImpl vm = VmImpl.require();
        MemoryImpl memory = getMemory();
        int coder = memory.load8(vm.stringCoderOffset, MemoryAtomicityMode.ACQUIRE);
        VmByteArrayImpl array = (VmByteArrayImpl) memory.loadRef(vm.stringValueOffset, MemoryAtomicityMode.ACQUIRE);
        Charset charset = coder == 0 ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_16BE;
        byte[] rawArray = array.getMemory().getArray();
        String newContent = new String(rawArray, vm.byteArrayContentOffset, array.getLength(), charset);
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
    public boolean contentEquals(String string) {
        return getContent().equals(string);
    }

    @Override
    protected VmStringImpl clone() {
        return new VmStringImpl(this);
    }
}
