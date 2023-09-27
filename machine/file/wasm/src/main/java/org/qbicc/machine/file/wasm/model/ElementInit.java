package org.qbicc.machine.file.wasm.model;

import java.util.Iterator;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Ops;

/**
 * An element initializer which uses a list of expressions to initialize each element.
 */
public record ElementInit(List<InsnSeq> items) {
    public ElementInit {
        Assert.checkNotNullParam("items", items);
        items = List.copyOf(items);
    }

    public int size() {
        return items.size();
    }

    public boolean isFuncList() {
        for (InsnSeq item : items) {
            Iterator<Insn<?>> iterator = item.iterator();
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
        }
        return true;
    }
}
