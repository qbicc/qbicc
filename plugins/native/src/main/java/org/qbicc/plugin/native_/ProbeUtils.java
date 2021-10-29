package org.qbicc.plugin.native_;

import org.qbicc.context.ClassContext;
import org.qbicc.context.Locatable;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.plugin.core.ConditionEvaluation;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.ArrayAnnotationValue;
import org.qbicc.type.annotation.StringAnnotationValue;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

final class ProbeUtils {
    private ProbeUtils() {}

    static void processInclude(ClassContext classContext, Locatable locatable, CProbe.Builder builder, Annotation include) {
        // include just one
        String str = ((StringAnnotationValue) include.getValue("value")).getString();
        // todo: when/unless (requires VM)
        if (ConditionEvaluation.get(classContext.getCompilationContext()).evaluateConditions(classContext, locatable, include)) {
            builder.include(str);
        }
    }

    static void processIncludeList(ClassContext classContext, Locatable locatable, CProbe.Builder builder, Annotation includeList) {
        ArrayAnnotationValue array = (ArrayAnnotationValue) includeList.getValue("value");
        int cnt = array.getElementCount();
        for (int j = 0; j < cnt; j ++) {
            processInclude(classContext, locatable, builder, (Annotation) array.getValue(j));
        }
    }

    static void processDefine(ClassContext classContext, Locatable locatable, CProbe.Builder builder, Annotation define) {
        // define just one
        String str = ((StringAnnotationValue) define.getValue("value")).getString();
        if (ConditionEvaluation.get(classContext.getCompilationContext()).evaluateConditions(classContext, locatable, define)) {
            builder.define(str);
        }
    }

    static void processDefineList(ClassContext classContext, Locatable locatable, CProbe.Builder builder, Annotation defineList) {
        ArrayAnnotationValue array = (ArrayAnnotationValue) defineList.getValue("value");
        int cnt = array.getElementCount();
        for (int j = 0; j < cnt; j ++) {
            processDefine(classContext, locatable, builder, (Annotation) array.getValue(j));
        }
    }

    static boolean processCommonAnnotation(ClassContext classContext, Locatable locatable, CProbe.Builder builder, Annotation annotation) {
        ClassTypeDescriptor annDesc = annotation.getDescriptor();
        if (annDesc.getPackageName().equals(Native.NATIVE_PKG)) {
            if (annDesc.getClassName().equals(Native.ANN_INCLUDE)) {
                processInclude(classContext, locatable, builder, annotation);
                return true;
            } else if (annDesc.getClassName().equals(Native.ANN_INCLUDE_LIST)) {
                processIncludeList(classContext, locatable, builder, annotation);
                return true;
            } else if (annDesc.getClassName().equals(Native.ANN_DEFINE)) {
                processDefine(classContext, locatable, builder, annotation);
                return true;
            } else if (annDesc.getClassName().equals(Native.ANN_DEFINE_LIST)) {
                processDefineList(classContext, locatable, builder, annotation);
                return true;
            }
        }
        return false;
    }
}
