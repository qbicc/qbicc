package cc.quarkus.vm.spi;

import static cc.quarkus.c_native.api.CNative.*;
import static cc.quarkus.c_native.stdc.Stdint.*;

/**
 *
 */
@internal
public class BciInfo extends object {
    /**
     * The number of bytes for which the index is valid (0 = 65536).  If the index is valid for more than
     * 65536 instructions, a second table entry is needed.
     */
    public uint16_t cnt;
    /**
     * The bytecode index.
     */
    public uint16_t bci;
}
