package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import cc.quarkus.qcc.machine.llvm.LLStruct;
import cc.quarkus.qcc.machine.llvm.LLValue;
import io.smallrye.common.constraint.Assert;

final class StructType extends AbstractValue implements LLStruct {
    final ArrayList<AbstractValue> members = new ArrayList<>();

    public LLStruct member(final LLValue type) {
        members.add((AbstractValue) Assert.checkNotNullParam("type", type));
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('{');
        final Iterator<AbstractValue> iterator = members.iterator();
        if (iterator.hasNext()) {
            target.append(' ');
            iterator.next().appendTo(target);
            while (iterator.hasNext()) {
                target.append(',');
                target.append(' ');
                iterator.next().appendTo(target);
            }
        }
        target.append(' ');
        target.append('}');
        return target;
    }
}
