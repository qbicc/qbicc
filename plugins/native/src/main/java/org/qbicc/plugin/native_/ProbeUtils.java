package org.qbicc.plugin.native_;

import org.qbicc.machine.probe.CProbe;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

final class ProbeUtils {
    private ProbeUtils() {}

    static void processInclude(CProbe.Builder builder, Annotation include) {
        // include just one
        String str = ((StringAnnotationValue) include.getValue("value")).getString();
        // todo: when/unless (requires VM)
        builder.include(str);
    }

    static void processIncludeList(CProbe.Builder builder, Annotation includeList) {
        ArrayAnnotationValue array = (ArrayAnnotationValue) includeList.getValue("value");
        int cnt = array.getElementCount();
        for (int j = 0; j < cnt; j ++) {
            processInclude(builder, (Annotation) array.getValue(j));
        }
    }

    static void processDefine(CProbe.Builder builder, Annotation define) {
        // define just one
        String str = ((StringAnnotationValue) define.getValue("value")).getString();
        // todo: when/unless (requires VM)
        builder.define(str);
    }

    static void processDefineList(CProbe.Builder builder, Annotation defineList) {
        ArrayAnnotationValue array = (ArrayAnnotationValue) defineList.getValue("value");
        int cnt = array.getElementCount();
        for (int j = 0; j < cnt; j ++) {
            processDefine(builder, (Annotation) array.getValue(j));
        }
    }

    static boolean processCommonAnnotation(CProbe.Builder builder, Annotation annotation) {
        ClassTypeDescriptor annDesc = annotation.getDescriptor();
        if (annDesc.getPackageName().equals(Native.NATIVE_PKG)) {
            if (annDesc.getClassName().equals(Native.ANN_INCLUDE)) {
                processInclude(builder, annotation);
                return true;
            } else if (annDesc.getClassName().equals(Native.ANN_INCLUDE_LIST)) {
                processIncludeList(builder, annotation);
                return true;
            } else if (annDesc.getClassName().equals(Native.ANN_DEFINE)) {
                processDefine(builder, annotation);
                return true;
            } else if (annDesc.getClassName().equals(Native.ANN_DEFINE_LIST)) {
                processDefineList(builder, annotation);
                return true;
            }
        }
        return false;
    }
}
