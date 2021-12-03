package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative.*;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.stdc.Stdint.*;

public class CompilerIntrinsics {
    @Hidden
    public static native Object emit_new_ref_array(type_id elemTypeId, uint8_t dimensions, int size);
}
