package org.qbicc.plugin.native_;

import static org.qbicc.runtime.CNative.*;

import java.util.List;
import java.util.function.IntFunction;

import org.qbicc.runtime.CNative;

final class Native {
    static final String C_NATIVE = className(CNative.class);

    static final String NATIVE_PKG = intName(CNative.class.getPackageName());

    static final String ANN_ALIGN = className(align.class);
    static final String ANN_ALIGN_LIST = className(align.List.class);
    static final String ANN_ALIGN_AS = className(align_as.class);
    static final String ANN_ALIGN_AS_LIST = className(align_as.List.class);
    static final String ANN_ARRAY_SIZE = className(array_size.class);
    static final String ANN_CONST = className(c_const.class);
    static final String ANN_CONSTRUCTOR = className(constructor.class);
    static final String ANN_DEFINE = className(define.class);
    static final String ANN_DEFINE_LIST = className(define.List.class);
    static final String ANN_DESTRUCTOR = className(destructor.class);
    static final String ANN_EXTERN = className(extern.class);
    static final String ANN_EXPORT = className(export.class);
    static final String ANN_INCLUDE = className(include.class);
    static final String ANN_INCLUDE_LIST = className(include.List.class);
    static final String ANN_INCOMPLETE = className(incomplete.class);
    static final String ANN_INTERNAL = className(internal.class);
    static final String ANN_LIB = className(lib.class);
    static final String ANN_LIB_LIST = className(lib.List.class);
    static final String ANN_MACRO = className(macro.class);
    static final String ANN_NAME = className(name.class);
    static final String ANN_NAME_LIST = className(name.List.class);
    static final String ANN_RESTRICT = className(restrict.class);
    static final String ANN_SIZE = className(size.class);
    static final String ANN_SIZE_LIST = className(size.List.class);
    static final String ANN_UNDEF = className(undef.class);
    static final String ANN_UNDEF_LIST = className(undef.List.class);

    static final String ARRAY_SIZE_INT_NAME = intName(array_size.class);
    static final String RESTRICT_INT_NAME = intName(restrict.class);
    static final String OBJECT_INT_NAME = intName(object.class);
    static final String STRUCT_INT_NAME = intName(struct.class);
    static final String UNION_INT_NAME = intName(union.class);
    static final String WORD_INT_NAME = intName(word.class);
    static final String TYPE_ID_INT_NAME = intName(type_id.class);
    static final String TYPE_ID = className(type_id.class);
    static final String HEADER_TYPE = className(header_type.class);
    static final String REFERENCE_INT_NAME = intName(reference.class);
    static final String REFERENCE = className(reference.class);
    static final String VOID = className(c_void.class);
    static final String PTR = className(ptr.class);
    static final String WORD = className(word.class);
    static final String OBJECT = className(object.class);
    static final String FUNCTION = className(function.class);
    static final String STATIC_METHOD = className(static_method.class);
    static final String INSTANCE_METHOD = className(instance_method.class);
    static final String C_NATIVE_INT_NAME = intName(CNative.class);
    static final String PTR_INT_NAME = intName(ptr.class);

    private static String className(Class<?> clz) {
        String name = clz.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private static String intName(String orig) {
        return orig.replace('.', '/');
    }

    private static String intName(Class<?> clz) {
        return intName(clz.getName());
    }

    static <T> List<T> copyWithPrefix(List<T> orig, T newVal, IntFunction<T[]> generator) {
        int size = orig.size();
        if (size == 0) {
            return List.of(newVal);
        } else if (size == 1) {
            return List.of(newVal, orig.get(0));
        } else if (size == 2) {
            return List.of(newVal, orig.get(0), orig.get(1));
        } else if (size == 3) {
            return List.of(newVal, orig.get(0), orig.get(1), orig.get(2));
        } else {
            T[] array = generator.apply(size + 1);
            array[0] = newVal;
            for (int i = 0; i < size; i ++) {
                array[i + 1] = orig.get(i);
            }
            return List.of(array);
        }
    }
}
