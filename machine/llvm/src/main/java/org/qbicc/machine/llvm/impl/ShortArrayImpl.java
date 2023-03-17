package org.qbicc.machine.llvm.impl;

import java.io.IOException;

final class ShortArrayImpl extends AbstractValue {
    final AbstractValue elementType;
    final short[] contents;

    ShortArrayImpl(AbstractValue elementType, short[] contents) {
        this.elementType = elementType;
        this.contents = contents;
    }

    private char digit(int val, boolean pad) {
        val %= 10;
        return val == 0 && pad ? ' ' : (char) ('0' + val);
    }

    private void writeVal(final Appendable target, final short item) throws IOException {
        int val = Short.toUnsignedInt(item);
        char digit = digit(val / 10000, true);
        target.append(digit);
        digit = digit(val / 1000, digit == ' ');
        target.append(digit);
        digit = digit(val / 100, digit == ' ');
        target.append(digit);
        digit = digit(val / 10, digit == ' ');
        target.append(digit);
        digit = digit(val, false);
        target.append(digit);
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('[');
        boolean multiLineOutput = false;
        if (contents.length > 8) {
            // complex types are easier to read if output across multiple lines
            multiLineOutput = true;
            target.append('\n');
        }
        int i = 0;
        if (i < contents.length) {
            target.append(' ');
            elementType.appendTo(target);
            target.append(' ');
            writeVal(target, contents[i]);
            i++;
            while (i < contents.length) {
                target.append(',');
                if (multiLineOutput && (i & 0x7) == 0) {
                    target.append(" ; ").append(String.valueOf(i - 8)).append(" \n");
                }
                target.append(' ');
                elementType.appendTo(target);
                target.append(' ');
                writeVal(target, contents[i]);
                i++;
            }
        }
        if (multiLineOutput) {
            target.append(" ; ").append(String.valueOf(i - 8)).append(" \n");
        }
        target.append(' ');
        target.append(']');
        return target;
    }
}
