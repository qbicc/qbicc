package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.runtime.CNative.*;

import cc.quarkus.qcc.runtime.CNative;

final class Native {
    static final String C_NATIVE = intName(CNative.class);

    static final String ANN_ALIGN = intName(align.class);
    static final String ANN_ALIGN_LIST = intName(align.List.class);
    static final String ANN_DEFINE = intName(define.class);
    static final String ANN_DEFINE_LIST = intName(define.List.class);
    static final String ANN_EXTERN = intName(extern.class);
    static final String ANN_EXPORT = intName(export.class);
    static final String ANN_INCLUDE = intName(include.class);
    static final String ANN_INCLUDE_LIST = intName(include.List.class);
    static final String ANN_LIB = intName(lib.class);
    static final String ANN_LIB_LIST = intName(lib.List.class);
    static final String ANN_NAME = intName(name.class);
    static final String ANN_SIZE = intName(size.class);
    static final String ANN_SIZE_LIST = intName(size.List.class);

    static final String OBJECT = intName(object.class);
    static final String WORD = intName(word.class);
    static final String PTR = intName(ptr.class);

    private static String intName(Class<?> clz) {
        return clz.getName().replace('.', '/');
    }
}
