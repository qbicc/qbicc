package org.qbicc.interpreter.impl;

import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * Singleton {@code VmClass} for {@code Class.class}.
 */
final class VmClassClassImpl extends VmClassImpl {
    VmClassClassImpl(VmImpl vm) {
        super(vm, vm.getCompilationContext().getBootstrapClassContext(), VmClassClassImpl.class);
    }

    @Override
    VmObjectImpl newInstance() {
        throw new UnsupportedOperationException("Cannot construct class objects");
    }

    /**
     * Construct a new class instance for the given type definition.
     *
     * @param typeDefinition the type definition for which a new class instance should be constructed (must not be {@code null})
     * @return the class (not {@code null})
     */
    VmClassImpl newInstance(LoadedTypeDefinition typeDefinition) {
        return newInstanceSearch(typeDefinition, typeDefinition);
    }

    private VmClassImpl newInstanceSearch(final LoadedTypeDefinition typeDefinition, final LoadedTypeDefinition searchTypeDef) {
        VmImpl vm = getVm();
        if (searchTypeDef == null) {
            return new VmClassImpl(vm, typeDefinition);
        } else if (searchTypeDef.getContext().isBootstrap()) {
            return switch (typeDefinition.getInternalName()) {
                case "java/lang/ClassLoader" -> new VmClassLoaderClassImpl(vm, typeDefinition);
                case "java/lang/Throwable" -> new VmThrowableClassImpl(vm, typeDefinition);
                case "java/lang/Thread" -> new VmThreadClassImpl(vm, typeDefinition);
                case "java/lang/Object" -> new VmClassImpl(vm, typeDefinition);
                default -> newInstanceSearch(typeDefinition, searchTypeDef.getSuperClass());
            };
        }
        return newInstanceSearch(typeDefinition, searchTypeDef.getSuperClass());
    }
}
