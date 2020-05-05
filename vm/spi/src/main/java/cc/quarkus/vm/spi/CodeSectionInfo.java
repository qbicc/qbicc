package cc.quarkus.vm.spi;

import static cc.quarkus.c_native.api.CNative.*;
import static cc.quarkus.c_native.stdc.Stdint.*;

/**
 * Information about a generated code section.
 */
@internal
public class CodeSectionInfo extends object {
    /**
     * The number of elements in the {@link #infos} array.
     */
    uint32_t count;
    /**
     * The sequential list of method instance information, starting at the base address of the code section and in
     * strictly increasing (binary-searchable) order.
     */
    MethodInstanceInfo[] infos;
}
