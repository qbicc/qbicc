package org.qbicc.machine.file.wasm.model;

import java.util.Iterator;
import java.util.List;

import org.qbicc.machine.file.wasm.Ops;
import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public sealed interface Element extends Named permits PassiveElement, ActiveElement, DeclarativeElement {
    RefType type();

    List<InsnSeq> init();

    default boolean isFuncList() {
        if (type() != RefType.funcref) {
            return false;
        }
        for (InsnSeq seq : init()) {
            Iterator<Insn<?>> iterator = seq.iterator();
            if (! iterator.hasNext()) {
                return false;
            }
            if (iterator.next().op() != Ops.ref.func) {
                return false;
            }
            if (! iterator.hasNext()) {
                return false;
            }
            if (iterator.next().op() != Ops.end) {
                return false;
            }
            if (iterator.hasNext()) {
                return false;
            }
        }
        return true;
    }
}
