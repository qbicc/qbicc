package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOError;
import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.LLValue;

abstract class AbstractEmittable implements Emittable {
    public abstract Appendable appendTo(Appendable target) throws IOException;

    static Appendable appendValue(Appendable target, LLValue value) throws IOException {
        return ((AbstractValue) value).appendTo(target);
    }

    public final StringBuilder toString(StringBuilder b) {
        try {
            appendTo(b);
        } catch (IOException e) {
            throw new IOError(e);
        }
        return b;
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    static char hexDigit(int val) {
        val &= 0xF;
        if (val <= 9) {
            return (char) ('0' + val);
        } else {
            return (char) ('A' - 10 + val);
        }
    }

    static <A extends Appendable> A appendHex(A target, int val) throws IOException {
        char v;
        if ((v = hexDigit(val >>> 28)) != '0') {
            target.append(v);
            target.append(hexDigit(val >>> 24));
            target.append(hexDigit(val >>> 20));
            target.append(hexDigit(val >>> 16));
            target.append(hexDigit(val >>> 12));
            target.append(hexDigit(val >>> 8));
            target.append(hexDigit(val >>> 4));
            target.append(hexDigit(val >>> 0));
        } else if ((v = hexDigit(val >>> 24)) != '0') {
            target.append(v);
            target.append(hexDigit(val >>> 20));
            target.append(hexDigit(val >>> 16));
            target.append(hexDigit(val >>> 12));
            target.append(hexDigit(val >>> 8));
            target.append(hexDigit(val >>> 4));
            target.append(hexDigit(val >>> 0));
        } else if ((v = hexDigit(val >>> 20)) != '0') {
            target.append(v);
            target.append(hexDigit(val >>> 16));
            target.append(hexDigit(val >>> 12));
            target.append(hexDigit(val >>> 8));
            target.append(hexDigit(val >>> 4));
            target.append(hexDigit(val >>> 0));
        } else if ((v = hexDigit(val >>> 16)) != '0') {
            target.append(v);
            target.append(hexDigit(val >>> 12));
            target.append(hexDigit(val >>> 8));
            target.append(hexDigit(val >>> 4));
            target.append(hexDigit(val >>> 0));
        } else if ((v = hexDigit(val >>> 12)) != '0') {
            target.append(v);
            target.append(hexDigit(val >>> 8));
            target.append(hexDigit(val >>> 4));
            target.append(hexDigit(val >>> 0));
        } else if ((v = hexDigit(val >>> 8)) != '0') {
            target.append(v);
            target.append(hexDigit(val >>> 4));
            target.append(hexDigit(val >>> 0));
        } else if ((v = hexDigit(val >>> 4)) != '0') {
            target.append(v);
            target.append(hexDigit(val >>> 0));
        } else {
            target.append(hexDigit(val));
        }
        return target;
    }

    static <A extends Appendable> A appendDecimal(A target, long val) throws IOException {
        target.append(Long.toString(val));
        return target;
    }

    static <A extends Appendable> A appendDecimal(A target, double val) throws IOException {
        target.append(Double.toString(val));
        return target;
    }

    static <A extends Appendable> A appendHex(A target, float val) throws IOException {
        target.append("0x" + Long.toHexString(Double.doubleToRawLongBits((double) val)));
        return target;
    }
}
