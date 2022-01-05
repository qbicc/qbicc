package org.qbicc.plugin.serialization;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 * This class enables VmObject instances to be modified
 * immediately before they are serialized.
 */
class InstanceInterceptor {
    private final CompilationContext ctxt;

    InstanceInterceptor(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

     VmObject process(VmObject original) {
        ClassTypeDescriptor otd = original.getVmClass().getTypeDefinition().getDescriptor();
        if (otd.packageAndClassNameEquals("java/io", "FileDescriptor")) {
            return interceptFileDescriptor(original);
        }

        return original;
    }


    private VmObject interceptFileDescriptor(VmObject original) {
        LoadedTypeDefinition fdType = original.getVmClass().getTypeDefinition();

        // Do not change already closed descriptors and the special descriptors stdin, stdout, stderr.
        if (original.getMemory().load32(original.indexOf(fdType.findField("fd")), SingleUnshared) < 3) {
            return original;
        }

        // For all other descriptors, update their backing Memory as-if they have been closed.
        original.getMemory().store32(original.indexOf(fdType.findField("fd")), -1, SingleUnshared);
        original.getMemory().store64(original.indexOf(fdType.findField("handle")), -1, SingleUnshared);
        original.getMemory().store8(original.indexOf(fdType.findField("closed")), 1, SingleUnshared);
        original.getMemory().storeRef(original.indexOf(fdType.findField("cleanup")), null, SingleUnshared);
        original.getMemory().storeRef(original.indexOf(fdType.findField("parent")), null, SingleUnshared);
        original.getMemory().storeRef(original.indexOf(fdType.findField("otherParents")), null, SingleUnshared);

        return original;
    }
}
