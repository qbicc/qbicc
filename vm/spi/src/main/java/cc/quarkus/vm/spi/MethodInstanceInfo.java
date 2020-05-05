package cc.quarkus.vm.spi;

import static cc.quarkus.c_native.api.CNative.*;
import static cc.quarkus.c_native.stdc.Stdint.*;

/**
 * An occurrence of a method in the run time image.  Methods may occur more than once due to inlining or specialization.
 * A method might be split into multiple pieces if another method is inlined within it.
 */
@internal
public class MethodInstanceInfo extends object {
    /**
     * The method information.
     */
    public ptr<MethodInfo> methodInfo;
    /**
     * The base address of this method occurrence.
     */
    public intptr_t address;
    /**
     * Number of entries in the BCI info table (max 65535).
     */
    public uint16_t bci_cnt;
    /**
     * Pointer of the address-to-BCI table.
     */
    public ptr<BciInfo> bcis;
}
