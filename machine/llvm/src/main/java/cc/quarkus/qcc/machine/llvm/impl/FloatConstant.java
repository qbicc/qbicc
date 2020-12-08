package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

/**
 * Floating-point constants in LLVM must not be inexact.
 *
 * See Section "Constants" in LLVM Language Reference Manual
 * http://llvm.org/docs/LangRef.html#simple-constants
 */
final class FloatConstant extends AbstractValue {
    final float value;

    FloatConstant(final float value) {
        this.value = value;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return appendHex(target, value);
    }
}
