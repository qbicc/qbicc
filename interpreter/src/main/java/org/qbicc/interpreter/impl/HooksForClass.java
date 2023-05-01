package org.qbicc.interpreter.impl;

import org.qbicc.context.ClassContext;
import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmArrayClass;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.generic.Signature;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

/**
 *
 */
final class HooksForClass {
    HooksForClass() {}

    @Hook
    static int getModifiers(VmThread thread, VmClass clazz) {
        return clazz.getTypeDefinition().getModifiers();
    }

    @Hook
    static VmClass getSuperclass(VmThread thread, VmClass clazz) {
        LoadedTypeDefinition sc = clazz.getTypeDefinition().getSuperClass();
        return sc == null ? null : sc.getVmClass();
    }

    @Hook
    static boolean isArray(VmThread thread, VmClass clazz) {
        return clazz instanceof VmArrayClass;
    }

    @Hook
    static boolean isHidden(VmThread thread, VmClass clazz) {
        return clazz.getTypeDefinition().isHidden();
    }

    @Hook
    static boolean isInterface(VmThread thread, VmClass clazz) {
        return ! (clazz instanceof VmPrimitiveClass) && clazz.getTypeDefinition().isInterface();
    }

    @Hook
    static boolean isAssignableFrom(VmThread thread, VmClass lhs, VmClass rhs) {
        return lhs.isAssignableFrom(rhs);
    }

    @Hook
    static boolean isPrimitive(VmThread thread, VmClass clazz) {
        return clazz instanceof VmPrimitiveClass;
    }

    @Hook
    static VmArray getEnclosingMethod0(VmThread thread, VmClass clazz) {
        if (clazz instanceof VmPrimitiveClass) {
            return null;
        }
        LoadedTypeDefinition def = clazz.getTypeDefinition();
        LoadedTypeDefinition emcDef = def.getEnclosingMethodClass();
        if (emcDef == null) {
            return null;
        }
        VmImpl vm = (VmImpl) thread.getVM();
        VmRefArrayClassImpl arrayClass = (VmRefArrayClassImpl) vm.objectClass.getArrayClass();
        VmRefArrayImpl vmArray = arrayClass.newInstance(3);
        ClassContext emcCtxt = emcDef.getContext();
        VmClassLoaderImpl emcLoader = vm.getClassLoaderForContext(emcCtxt);
        VmClassImpl emc = emcLoader.loadClass(emcDef.getInternalName());
        VmObject[] realArray = vmArray.getArray();
        realArray[0] = emc;
        MethodElement enclosingMethod = def.getEnclosingMethod();
        if (enclosingMethod != null) {
            realArray[1] = vm.intern(enclosingMethod.getName());
            realArray[2] = vm.intern(enclosingMethod.getDescriptor().toString());
        }
        return vmArray;
    }

    @Hook
    static VmClass getDeclaringClass0(VmThread thread, VmClass clazz) {
        // todo: this one probably should just be a single field on Class
        if (clazz instanceof VmPrimitiveClass) {
            return null;
        }
        LoadedTypeDefinition def = clazz.getTypeDefinition();
        NestedClassElement enc = def.getEnclosingNestedClass();
        if (enc != null) {
            DefinedTypeDefinition enclosingType = enc.getEnclosingType();
            if (enclosingType != null) {
                VmImpl vm = (VmImpl) thread.getVM();
                VmClassLoaderImpl loader = vm.getClassLoaderForContext(enclosingType.getContext());
                return loader.loadClass(enclosingType.getInternalName());
            }
        }
        return null;
    }

    @Hook
    VmReferenceArray getDeclaredClasses0(VmThread thread, VmClass clazz) {
        VmImpl vm = (VmImpl) thread.getVM();
        if (clazz instanceof VmPrimitiveClass || clazz instanceof VmArrayClass) {
            return vm.newArrayOf(vm.classClass, 0);
        }
        LoadedTypeDefinition ltd = clazz.getTypeDefinition();
        int ncc = ltd.getEnclosedNestedClassCount();
        VmReferenceArray result = vm.newArrayOf(vm.classClass, ncc);
        for (int i=0; i<ncc; i++) {
            result.store(i, ltd.getEnclosedNestedClass(i).getCorrespondingType().load().getVmClass());
        }
        return result;
    }

    @Hook
    static VmArray getInterfaces0(VmThreadImpl thread, VmClass clazz) {
        LoadedTypeDefinition ltd = clazz.getTypeDefinition();
        LoadedTypeDefinition[] ltdInt = ltd.getInterfaces();
        VmClass[] interfaces = new VmClass[ltdInt.length];
        for (int i=0; i<ltdInt.length; i++) {
            interfaces[i] = ltdInt[i].getVmClass();
        }
        return thread.vm.newArrayOf(thread.vm.classClass, interfaces);
    }

    @Hook
    static boolean isInstance(VmThread thread, VmClass clazz, VmObject obj) {
        return obj != null && obj.getVmClass().getInstanceObjectType().isSubtypeOf(clazz.getInstanceObjectType());
    }

    @Hook
    static VmClass forName0(VmThreadImpl thread, VmString nameObj, boolean initialize, VmClassLoader loader, VmClass caller) {
        if (loader == null) {
            loader = thread.vm.bootstrapClassLoader;
        }
        String name = nameObj.getContent();
        VmClassImpl clazz;
        if (name.startsWith("[")) {
            int dims = 0;
            while (name.startsWith("[")) {
                name = name.substring(1);
                dims++;
            }
            if (name.startsWith("L") && name.endsWith(";")) {
                clazz = (VmClassImpl) loader.loadClass(name.substring(1, name.length() - 1));
            } else {
                clazz = switch (name) {
                    case "B" -> thread.vm.byteClass;
                    case "Z" -> thread.vm.booleanClass;
                    case "C" -> thread.vm.charClass;
                    case "S" -> thread.vm.shortClass;
                    case "I" -> thread.vm.intClass;
                    case "F" -> thread.vm.floatClass;
                    case "J" -> thread.vm.longClass;
                    case "D" -> thread.vm.doubleClass;
                    default -> throw new Thrown(thread.vm.noClassDefFoundErrorClass.newInstance("Bad array descriptor"));
                };
            }
            for (int i = 0; i < dims; i ++) {
                clazz = clazz.getArrayClass();
            }
        } else {
            clazz = (VmClassImpl) loader.loadClass(name);
        }

        if (initialize) {
            clazz.initialize(thread);
        }
        return clazz;
    }

    @Hook
    static VmString getGenericSignature0(VmThread thread, VmClass clazz) {
        // TODO:  We can eliminate this hook when we upgrade to classlib 0.49
        FieldElement f = clazz.getVmClass().getTypeDefinition().findField("genericSignature");
        return (VmString)clazz.getMemory().loadRef(clazz.indexOf(f), SinglePlain);
    }
}
