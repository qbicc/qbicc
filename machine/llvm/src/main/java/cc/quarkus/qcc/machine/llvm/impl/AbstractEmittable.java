package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIFlags;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISPFlags;

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

    static <A extends Appendable> A appendHex(A target, double val) throws IOException {
        target.append("0x" + Long.toHexString(Double.doubleToRawLongBits((double) val)));
        return target;
    }

    static <A extends Appendable> A appendHex(A target, float val) throws IOException {
        return appendHex(target, (double) val);
    }

    static <A extends Appendable> A appendEscapedString(A target, String val) throws IOException {
        byte[] valBytes = val.getBytes(StandardCharsets.UTF_8);

        target.append('"');
        for (byte b : valBytes) {
            if (b < ' ' || b > '~' || b == '"' || b == '\\') {
                target.append('\\');
                target.append(hexDigit(b >>> 4));
                target.append(hexDigit(b & 0xf));
            } else {
                target.append((char)b);
            }
        }
        target.append('"');

        return target;
    }

    static <A extends Appendable> A appendDiFlags(A target, EnumSet<DIFlags> flags) throws IOException {
        if (flags.isEmpty()) {
            target.append("DIFlagZero");
        } else {
            boolean first = true;

            for (DIFlags flag : flags) {
                if (first) {
                    first = false;
                } else {
                    target.append(" | ");
                }

                target.append(flag.name);
            }
        }

        return target;
    }

    static <A extends Appendable> A appendDiSpFlags(A target, EnumSet<DISPFlags> flags) throws IOException {
        if (flags.isEmpty()) {
            target.append("DISPFlagZero");
        } else {
            boolean first = true;

            for (DISPFlags flag : flags) {
                if (first) {
                    first = false;
                } else {
                    target.append(" | ");
                }

                target.append(flag.name);
            }
        }

        return target;
    }
}
