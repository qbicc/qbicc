package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import cc.quarkus.qcc.machine.llvm.Array;
import cc.quarkus.qcc.machine.llvm.LLValue;

final class ArrayImpl extends AbstractValue implements Array {
    final AbstractValue elementType;
    final ArrayList<AbstractValue> values = new ArrayList<>();

    ArrayImpl(final LLValue elementType) {
        this.elementType = (AbstractValue) elementType;
    }

    public Array item(final LLValue value) {
        values.add((AbstractValue) value);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('[');
        Iterator<AbstractValue> iterator = values.iterator();
        if (iterator.hasNext()) {
            target.append(' ');
            elementType.appendTo(target);
            target.append(' ');
            iterator.next().appendTo(target);
            while (iterator.hasNext()) {
                target.append(',');
                target.append(' ');
                elementType.appendTo(target);
                target.append(' ');
                iterator.next().appendTo(target);
            }
        }
        target.append(' ');
        target.append(']');
        return target;
    }
}
