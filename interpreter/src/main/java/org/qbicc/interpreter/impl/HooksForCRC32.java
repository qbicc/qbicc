package org.qbicc.interpreter.impl;

import java.util.zip.CRC32;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForCRC32 {

    private final VmClassImpl crc32;

    HooksForCRC32(VmImpl vm) {
        crc32 = vm.bootstrapClassLoader.loadClass("java/util/zip/CRC32");
    }

    @Hook
    void reset(VmThread thread, VmObjectImpl obj) {
        CRC32 crc = obj.getOrAddAttachment(CRC32.class, CRC32::new);
        crc.reset();
        obj.setIntField(crc32.getTypeDefinition(), "crc", 0);
    }

    @Hook(descriptor = "([BII)V")
    void update(VmThread thread, VmObjectImpl t, VmByteArrayImpl a, int off, int len) {
        CRC32 crc = t.getOrAddAttachment(CRC32.class, CRC32::new);
        crc.update(a.getArray(), off, len);
        t.setIntField(crc32.getTypeDefinition(), "crc", (int) crc.getValue());
    }

    @Hook(descriptor = "(I)V")
    void update(VmThread thread, VmObjectImpl t, int val) {
        CRC32 crc = t.getOrAddAttachment(CRC32.class, CRC32::new);
        crc.update(val);
        t.setIntField(crc32.getTypeDefinition(), "crc", (int) crc.getValue());
    }
}
