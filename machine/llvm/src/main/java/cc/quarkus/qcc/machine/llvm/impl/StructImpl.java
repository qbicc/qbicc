package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Struct;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class StructImpl extends AbstractValue implements Struct {
    private final ArrayList<AbstractValue> pairs = new ArrayList<>();

    StructImpl() {}

    public Struct item(final LLValue type, final LLValue value) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        pairs.add((AbstractValue) type);
        pairs.add((AbstractValue) value);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('{');
        final Iterator<AbstractValue> iterator = pairs.iterator();
        if (iterator.hasNext()) {
            target.append(' ');
            iterator.next().appendTo(target);
            target.append(' ');
            iterator.next().appendTo(target);
            while (iterator.hasNext()) {
                target.append(',');
                target.append(' ');
                iterator.next().appendTo(target);
                target.append(' ');
                iterator.next().appendTo(target);
            }
        }
        target.append(' ');
        target.append('}');
        return target;
    }
}
