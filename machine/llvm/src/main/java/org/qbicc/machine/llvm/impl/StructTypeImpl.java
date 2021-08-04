package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.StructType;
import io.smallrye.common.constraint.Assert;

final class StructTypeImpl extends AbstractValue implements StructType {
    final ArrayList<AbstractValue> members = new ArrayList<>();
    final ArrayList<String> memberNames = new ArrayList<>();

    public StructType member(final LLValue type, final String name) {
        members.add((AbstractValue) Assert.checkNotNullParam("type", type));
        memberNames.add(name);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('{');
        final Iterator<AbstractValue> iterator = members.iterator();
        final Iterator<String> nameIterator = memberNames.iterator();
        if (iterator.hasNext()) {
            target.append(' ');
            iterator.next().appendTo(target).append(" ; " + nameIterator.next());

            while (iterator.hasNext()) {
                target.append('\n').append('\t');
                target.append(',');
                target.append(' ');
                iterator.next().appendTo(target).append(" ; " + nameIterator.next());
            }
        }
        target.append('\n').append('\t');
        target.append(' ');
        target.append('}');
        return target;
    }
}
