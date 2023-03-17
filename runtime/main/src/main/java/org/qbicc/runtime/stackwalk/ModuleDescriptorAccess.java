package org.qbicc.runtime.stackwalk;

import java.lang.module.ModuleDescriptor;

import org.qbicc.runtime.patcher.PatchClass;

/**
 *
 */
@PatchClass(ModuleDescriptor.class)
class ModuleDescriptorAccess {
    String rawVersionString;
}
