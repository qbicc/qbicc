package cc.quarkus.vm.spi;

import static cc.quarkus.c_native.api.CNative.*;

import cc.quarkus.api.Intrinsic;

/**
 *
 */
public final class classId extends word {
    @Intrinsic
    public native Class<?> asClassConstant();

    @Intrinsic
    public static native classId fromClassConstant(Class<?> clazz);
}
