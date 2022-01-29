package org.qbicc.plugin.patcher;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
final class InitializerPatchInfo extends ExecutableMemberPatchInfo implements Locatable {
    private final InitializerResolver initializerResolver;

    InitializerPatchInfo(final int index, InitializerResolver initializerResolver, final String internalName, final Annotation annotation) {
        super(index, ClassFile.ACC_STATIC, internalName, annotation, null);
        this.initializerResolver = initializerResolver;
    }

    public InitializerResolver getInitializerResolver() {
        return initializerResolver;
    }

    MethodDescriptor getDescriptor() {
        return MethodDescriptor.VOID_METHOD_DESCRIPTOR;
    }

    @Override
    public Location getLocation() {
        return ClassContextPatchInfo.getMethodLocation("<clinit>", getInternalName());
    }
}
