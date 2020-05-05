package cc.quarkus.vm.spi;

import static cc.quarkus.c_native.api.CNative.*;

/**
 *
 */
@internal
public final class MethodInfo extends object {
    public ptr<?> namePtr;
    public ptr<LineInfo> lines;
}
