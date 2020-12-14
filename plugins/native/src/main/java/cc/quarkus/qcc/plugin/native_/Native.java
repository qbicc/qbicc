package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.runtime.CNative.*;

import cc.quarkus.qcc.runtime.CNative;

final class Native {
    static final String C_NATIVE = className(CNative.class);

    static final String NATIVE_PKG = intName(CNative.class.getPackageName());

    static final String ANN_ALIGN = className(align.class);
    static final String ANN_ALIGN_LIST = className(align.List.class);
    static final String ANN_DEFINE = className(define.class);
    static final String ANN_DEFINE_LIST = className(define.List.class);
    static final String ANN_EXTERN = className(extern.class);
    static final String ANN_EXPORT = className(export.class);
    static final String ANN_INCLUDE = className(include.class);
    static final String ANN_INCLUDE_LIST = className(include.List.class);
    static final String ANN_LIB = className(lib.class);
    static final String ANN_LIB_LIST = className(lib.List.class);
    static final String ANN_NAME = className(name.class);
    static final String ANN_SIZE = className(size.class);
    static final String ANN_SIZE_LIST = className(size.List.class);
    static final String ANN_CONST = className(c_const.class);
    static final String ANN_RESTRICT = className(restrict.class);
    static final String ANN_ARRAY_SIZE = className(array_size.class);

    static final String OBJECT_INT_NAME = intName(object.class.getName());
    static final String WORD_INT_NAME = intName(word.class.getName());
    static final String PTR = className(ptr.class);
    static final String FUNCTION = className(function.class);

    private static String className(Class<?> clz) {
        String name = clz.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private static String intName(String orig) {
        return orig.replace('.', '/');
    }
}
