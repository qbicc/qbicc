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
    final boolean isIdentified; /* spacing and name comments will be added to identified structures for readability. */

    StructTypeImpl(boolean isIdentified) {
        this.isIdentified = isIdentified;
    }

    public StructType member(final LLValue type, final String name) {
        members.add((AbstractValue) Assert.checkNotNullParam("type", type));
        memberNames.add(name);
        return this;
    }

    public boolean literalPaddingMember(String name) {
        return !isIdentified && name.equals("padding");
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        final Iterator<AbstractValue> iterator = members.iterator();
        final Iterator<String> nameIterator = memberNames.iterator();
        String name;
        AbstractValue value;

        target.append('{');
        if (iterator.hasNext()) {
            name = nameIterator.next();
            value = iterator.next();
            /* don't explicitly print padding for literal types */
            if (!literalPaddingMember(name)) {
                target.append(' ');
                value.appendTo(target);
                if (isIdentified) {
                    target.append(" ; " + name);
                }
            }

            while (iterator.hasNext()) {
                name = nameIterator.next();
                value = iterator.next();
                if (!literalPaddingMember(name)) {
                    if (isIdentified) {
                        target.append('\n').append('\t');
                    }
                    target.append(',');
                    target.append(' ');
                    value.appendTo(target);
                    if (isIdentified) {
                        target.append(" ; " + name);
                    }
                }
            }
        }
        if (isIdentified) {
            target.append('\n').append('\t');
        }
        target.append(' ');
        target.append('}');
        return target;
    }
}
