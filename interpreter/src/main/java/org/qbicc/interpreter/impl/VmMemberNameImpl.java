package org.qbicc.interpreter.impl;

import java.util.ArrayList;

import org.qbicc.context.ClassContext;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

/**
 *
 */
class VmMemberNameImpl extends VmObjectImpl {
///*non-public*/ final class ResolvedMethodName {
//    //@Injected JVM_Method* vmtarget;
//    //@Injected Class<?>    vmholder;
//};
//
///*non-public*/ final class MemberName implements Member, Cloneable {
//    private Class<?> clazz;       // class in which the member is defined
//    private String   name;        // may be null if not yet materialized
//    private Object   type;        // may be null if not yet materialized
//    private int      flags;       // modifier bits; see reflect.Modifier
//    private ResolvedMethodName method;    // cached resolved method information
//    //@Injected intptr_t       vmindex;   // vtable index or offset of resolved member
//    Object   resolution;  // if null, this guy is resolved

    private volatile Element resolved;

    VmMemberNameImpl(VmClassImpl clazz) {
        super(clazz);
    }

    VmMemberNameImpl(VmMemberNameImpl original) {
        super(original);
    }

    @Override
    protected VmMemberNameImpl clone() {
        return new VmMemberNameImpl(this);
    }

    private static final int IS_METHOD = 1 << 16;
    private static final int IS_CTOR = 1 << 17;
    private static final int IS_FIELD = 1 << 18;
    private static final int IS_TYPE = 1 << 19;

    void resolve(VmThreadImpl thread, VmClassImpl caller, boolean speculativeResolve) {
        if (resolved == null) {
            LoadedTypeDefinition def = getVmClass().getTypeDefinition();
            VmClassImpl clazz = (VmClassImpl) getMemory().loadRef(indexOf(def.findField("clazz")), SinglePlain);
            VmStringImpl name = (VmStringImpl) getMemory().loadRef(indexOf(def.findField("name")), SinglePlain);
            VmObjectImpl type = (VmObjectImpl) getMemory().loadRef(indexOf(def.findField("type")), SinglePlain);
            int flags = getMemory().load32(indexOf(def.findField("flags")), SinglePlain);
            if (clazz == null || name == null || type == null) {
                throw new Thrown(thread.vm.linkageErrorClass.newInstance("Null name or class"));
            }
            ClassContext classContext;
            VmClassLoader classLoader;
            if (caller == null || (classLoader = caller.getClassLoader()) == null) {
                classLoader = thread.vm.getBootstrapClassLoader();
            }
            classContext = classLoader.getClassContext();
            // determine what kind of thing we're resolving
            if ((flags & IS_FIELD) != 0) {
                // find a field with the given name
                resolved = clazz.getTypeDefinition().findField(name.getContent());
                if (resolved == null && ! speculativeResolve) {
                    throw new Thrown(thread.vm.linkageErrorClass.newInstance("No such field: " + clazz.getName() + "#" + name.getContent()));
                }
            } else if ((flags & IS_TYPE) != 0) {
                throw new Thrown(thread.vm.linkageErrorClass.newInstance("Not sure what to do for resolving a type"));
            } else {
                // some kind of exec element
                MethodDescriptor desc = createFromMethodType(classContext, thread, type);
                if (((flags & IS_CTOR) != 0)) {
                    int idx = clazz.getTypeDefinition().findConstructorIndex(desc);
                    if (idx == -1) {
                        if (! speculativeResolve) {
                            throw new Thrown(thread.vm.linkageErrorClass.newInstance("No such constructor: " + name.getContent() + ":" + desc.toString()));
                        }
                    } else {
                        resolved = clazz.getTypeDefinition().getConstructor(idx);
                    }
                } else if (((flags & IS_METHOD) != 0)){
                    int idx = clazz.getTypeDefinition().findMethodIndex(name.getContent(), desc);
                    if (idx == -1) {
                        if (! speculativeResolve) {
                            throw new Thrown(thread.vm.linkageErrorClass.newInstance("No such method: " + clazz.getName() + "#" + name.getContent() + ":" + desc.toString()));
                        }
                    } else {
                        resolved = clazz.getTypeDefinition().getMethod(idx);
                    }
                } else {
                    throw new Thrown(thread.vm.linkageErrorClass.newInstance("Unknown resolution request"));
                }
            }
        }
    }

    Element getResolved() {
        return resolved;
    }

    MethodDescriptor createFromMethodType(ClassContext classContext, VmThreadImpl thread, VmObjectImpl methodType) {
        VmClassImpl mtClass = methodType.getVmClass();
        if (! mtClass.getName().equals("java.lang.invoke.MethodType")) {
            throw new Thrown(thread.vm.linkageErrorClass.newInstance("Type argument is of wrong class"));
        }
        LoadedTypeDefinition mtDef = mtClass.getTypeDefinition();
        VmClassImpl rtype = (VmClassImpl) methodType.getMemory().loadRef(indexOf(mtDef.findField("rtype")), SinglePlain);
        if (rtype == null) {
            throw new Thrown(thread.vm.linkageErrorClass.newInstance("MethodType has null return type"));
        }
        VmRefArrayImpl ptypes = (VmRefArrayImpl) methodType.getMemory().loadRef(indexOf(mtDef.findField("ptypes")), SinglePlain);
        if (ptypes == null) {
            throw new Thrown(thread.vm.linkageErrorClass.newInstance("MethodType has null param types"));
        }
        int pcnt = ptypes.getLength();
        ArrayList<TypeDescriptor> paramTypes = new ArrayList<>(pcnt);
        for (int i = 0; i < pcnt; i ++) {
            VmClassImpl clazz = (VmClassImpl) ptypes.getMemory().loadRef(ptypes.getArrayElementOffset(i), SinglePlain);
            paramTypes.add(clazz.getDescriptor());
        }
        return MethodDescriptor.synthesize(classContext, rtype.getDescriptor(), paramTypes);
    }
}
