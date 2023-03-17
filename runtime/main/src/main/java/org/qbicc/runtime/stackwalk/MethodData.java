package org.qbicc.runtime.stackwalk;

import static org.qbicc.runtime.CNative.c_const;
import static org.qbicc.runtime.CNative.ptr;
import static org.qbicc.runtime.stackwalk.CallSiteTable.*;

import org.qbicc.runtime.AutoQueued;

public final class MethodData {

    @AutoQueued
    public static void fillStackTraceElements(StackTraceElement[] steArray, Object backtrace, int depth) {
        int[] sourceCodeIndexList = (int[]) backtrace;
        for (int i = 0; i < depth; i++) {
            int sc = sourceCodeIndexList[i];
            final ptr<@c_const struct_source> sourceInfo = getSourceInfo(sc);
            final ptr<@c_const struct_subprogram> methodInfo = getMethodInfo(sourceInfo);
            Class<?> clazz = getEnclosingTypeClass(methodInfo);
            Module module = clazz.getModule();
            String modName;
            String modVer;
            if (module == null) {
                modName = null;
                modVer = null;
            } else {
                final ModuleDescriptorAccess descriptor = (ModuleDescriptorAccess) (Object) module.getDescriptor();
                if (descriptor == null) {
                    modName = null;
                    modVer = null;
                } else {
                    modName = module.getName();
                    modVer = descriptor.rawVersionString;
                }
            }
            ClassLoader classLoader = clazz.getClassLoader();
            String classLoaderName = classLoader == null ? null : classLoader.getName();
            steArray[i] = new StackTraceElement(classLoaderName, modName, modVer, clazz.getName(), getMethodName(methodInfo), getMethodFileName(methodInfo), getSourceLine(sourceInfo));
            ((StackTraceElementAccess)(Object)steArray[i]).declaringClassObject = clazz;
        }
    }
}

