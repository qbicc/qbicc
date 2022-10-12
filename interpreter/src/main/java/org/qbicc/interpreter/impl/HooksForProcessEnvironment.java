package org.qbicc.interpreter.impl;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForProcessEnvironment {

    private final VmClassImpl stringClass;

    HooksForProcessEnvironment(VmImpl vm) {
        stringClass = vm.bootstrapClassLoader.loadClass("java/lang/String");
    }

    @Hook
    VmArray getHostEnvironment(VmThread thread) {
        // todo: customize
        VmReferenceArray array = (VmReferenceArray) stringClass.getArrayClass().newInstance(4);
        VmObject[] arrayArray = array.getArray();
        int i = 0;
        for (String str : List.of(
            "TZ", TimeZone.getDefault().getDisplayName(),
            "LANG", Locale.getDefault().toLanguageTag() + "." + Charset.defaultCharset().name()
        )) {
            arrayArray[i++] = thread.getVM().intern(str);
        }
        return array;
    }
}
