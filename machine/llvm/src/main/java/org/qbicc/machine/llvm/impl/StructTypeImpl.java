package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.StructType;
import io.smallrye.common.constraint.Assert;

final class StructTypeImpl extends AbstractValue implements StructType {
    final ArrayList<AbstractValue> members = new ArrayList<>();

    public StructType member(final LLValue type) {
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
