package org.qbicc.plugin.native_;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

    static final class ProbeProcessor implements Consumer<CProbe.Builder> {
        private final ClassContext classContext;
        private final Locatable locatable;
        private List<Annotation> defines = List.of();
        private List<Annotation> undefs = List.of();
        private List<Annotation> includes = List.of();

        ProbeProcessor(ClassContext classContext, Locatable locatable) {
            this.classContext = classContext;
            this.locatable = locatable;
        }

        public boolean processAnnotation(Annotation annotation) {
            ClassTypeDescriptor annDesc = annotation.getDescriptor();
            if (annDesc.getPackageName().equals(Native.NATIVE_PKG)) {
                if (annDesc.getClassName().equals(Native.ANN_INCLUDE)) {
                    processInclude(annotation);
                    return true;
                } else if (annDesc.getClassName().equals(Native.ANN_INCLUDE_LIST)) {
                    ArrayAnnotationValue array = (ArrayAnnotationValue) annotation.getValue("value");
                    int cnt = array.getElementCount();
                    for (int j = 0; j < cnt; j ++) {
                        processInclude((Annotation) array.getValue(j));
                    }
                    return true;
                } else if (annDesc.getClassName().equals(Native.ANN_DEFINE)) {
                    processDefine(annotation);
                    return true;
                } else if (annDesc.getClassName().equals(Native.ANN_DEFINE_LIST)) {
                    ArrayAnnotationValue array = (ArrayAnnotationValue) annotation.getValue("value");
                    int cnt = array.getElementCount();
                    for (int j = 0; j < cnt; j ++) {
                        processDefine((Annotation) array.getValue(j));
                    }
                    return true;
                } else if (annDesc.getClassName().equals(Native.ANN_UNDEF)) {
                    processUndef(annotation);
                    return true;
                } else if (annDesc.getClassName().equals(Native.ANN_UNDEF_LIST)) {
                    ArrayAnnotationValue array = (ArrayAnnotationValue) annotation.getValue("value");
                    int cnt = array.getElementCount();
                    for (int j = 0; j < cnt; j ++) {
                        processUndef((Annotation) array.getValue(j));
                    }
                    return true;
                }
            }
            return false;
        }

        private void processInclude(final Annotation annotation) {
            if (includes.isEmpty()) {
                includes = List.of(annotation);
            } else if (includes.size() == 1) {
                includes = List.of(includes.get(0), annotation);
            } else if (includes.size() == 2) {
                includes = new ArrayList<>(includes);
                includes.add(annotation);
            } else {
                includes.add(annotation);
            }
        }

        private void processDefine(final Annotation annotation) {
            if (defines.isEmpty()) {
                defines = List.of(annotation);
            } else if (defines.size() == 1) {
                defines = List.of(defines.get(0), annotation);
            } else if (defines.size() == 2) {
                defines = new ArrayList<>(defines);
                defines.add(annotation);
            } else {
                defines.add(annotation);
            }
        }

        private void processUndef(final Annotation annotation) {
            if (undefs.isEmpty()) {
                undefs = List.of(annotation);
            } else if (undefs.size() == 1) {
                undefs = List.of(undefs.get(0), annotation);
            } else if (undefs.size() == 2) {
                undefs = new ArrayList<>(undefs);
                undefs.add(annotation);
            } else {
                undefs.add(annotation);
            }
        }

        @Override
        public void accept(CProbe.Builder builder) {
            // defines first, then undefs, then includes
            Map<String, String> globalDefs = classContext.getCompilationContext().getPlatform().os().globalDefinitions();
            for (Map.Entry<String, String> entry : globalDefs.entrySet()) {
                String value = entry.getValue();
                if (value.isEmpty()) {
                    builder.define(entry.getKey());
                } else {
                    builder.define(entry.getKey(), value);
                }
            }
            for (Annotation define : defines) {
                String str = ((StringAnnotationValue) define.getValue("value")).getString();
                if (ConditionEvaluation.get(classContext.getCompilationContext()).evaluateConditions(classContext, locatable, define)) {
                    if (define.getValue("as") instanceof StringAnnotationValue asStr) {
                        builder.define(str, asStr.getString());
                    } else {
                        builder.define(str);
                    }
                }
            }
            for (Annotation undef : undefs) {
                String str = ((StringAnnotationValue) undef.getValue("value")).getString();
                if (ConditionEvaluation.get(classContext.getCompilationContext()).evaluateConditions(classContext, locatable, undef)) {
                    builder.undef(str);
                }
            }
            for (Annotation include : includes) {
                String str = ((StringAnnotationValue) include.getValue("value")).getString();
                if (ConditionEvaluation.get(classContext.getCompilationContext()).evaluateConditions(classContext, locatable, include)) {
                    builder.include(str);
                }
            }
        }
    }
}
