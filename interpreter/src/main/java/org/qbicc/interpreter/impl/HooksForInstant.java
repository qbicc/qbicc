package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.Hook;
import org.qbicc.type.definition.element.MethodElement;

import java.util.List;

public class HooksForInstant {
    final VmClassImpl instantClass;
    final MethodElement ofEpochMilli;

    HooksForInstant(VmImpl vm) {
        this.instantClass = vm.getBootstrapClassLoader().loadClass("java/time/Instant");
        ofEpochMilli = instantClass.getTypeDefinition().requireSingleMethod("ofEpochMilli", 1);
    }

    @Hook(descriptor = "()Ljava/time/Instant;")
    Object now(VmThreadImpl thread) {
        long currentTimeMillis = System.currentTimeMillis();
        Object instant = thread.getVM().invokeExact(ofEpochMilli, null, List.of(currentTimeMillis));
        return instant;
    }
}
