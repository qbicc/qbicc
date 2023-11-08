package org.qbicc.plugin.wasm;

import static org.qbicc.machine.file.wasm.Ops.*;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.machine.file.wasm.Data;
import org.qbicc.machine.file.wasm.model.ActiveSegment;
import org.qbicc.machine.file.wasm.model.DefinedMemory;
import org.qbicc.machine.file.wasm.model.ImportedMemory;
import org.qbicc.machine.file.wasm.model.InsnSeq;
import org.qbicc.machine.file.wasm.model.Memory;
import org.qbicc.machine.file.wasm.model.PassiveSegment;
import org.qbicc.machine.file.wasm.model.Segment;

/**
 * The WASM-specific attachment.
 */
public final class Wasm {
    private final Memory globalMemory;
    private final Memory heapMemory;
    private final Memory threadLocalMemory;

    private final Segment globalData;
    private final Segment initialHeap;
    private final Segment initialThreadLocal;

    private Wasm() {
        // todo: mutable memory info
        globalMemory = new ImportedMemory("env", "globalMemory", 0, Long.MAX_VALUE, true);
        heapMemory = new ImportedMemory("env", "heapMemory", 0, Long.MAX_VALUE, true);
        threadLocalMemory = new DefinedMemory(0, 0x1_000, false);
        globalData = new PassiveSegment("globalData", Data.of(new byte[0]));
        initialHeap = new PassiveSegment("initialHeap", Data.of(new byte[0]));
        InsnSeq offsetSeq = new InsnSeq();
        offsetSeq.add(i32.const_, 0);
        offsetSeq.end();
        initialThreadLocal = new ActiveSegment("initialThreadLocal", Data.of(new byte[0]), offsetSeq);
    }

    private static final AttachmentKey<Wasm> KEY = new AttachmentKey<>();

    public static Wasm get(CompilationContext ctxt) {
        Wasm wasm = ctxt.getAttachment(KEY);
        if (wasm == null) {
            wasm = new Wasm();
            Wasm appearing = ctxt.putAttachmentIfAbsent(KEY, wasm);
            if (appearing != null) {
                wasm = appearing;
            }
        }
        return wasm;
    }

    public Memory globalMemory() {
        return globalMemory;
    }

    public Memory heapMemory() {
        return heapMemory;
    }

    public Memory threadLocalMemory() {
        return threadLocalMemory;
    }

    public Segment globalData() {
        return globalData;
    }

    public Segment initialHeap() {
        return initialHeap;
    }

    public Segment initialThreadLocal() {
        return initialThreadLocal;
    }
}
