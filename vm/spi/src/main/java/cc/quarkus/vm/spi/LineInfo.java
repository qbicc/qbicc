package cc.quarkus.vm.spi;

import static cc.quarkus.c_native.api.CNative.*;
import static cc.quarkus.c_native.stdc.Stdint.*;

/**
 *
 */
@internal
public class LineInfo extends object {
    public intptr_t start;
    public uint16_t length;
    public uint16_t bci;
    public uint32_t line;
}
