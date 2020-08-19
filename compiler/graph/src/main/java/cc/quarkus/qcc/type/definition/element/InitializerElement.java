package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 *
 */
public interface InitializerElement extends ExactExecutableElement {

    InitializerElement EMPTY = makeEmpty();

    private static InitializerElement makeEmpty() {
        Builder builder = builder();
        builder.setModifiers(ClassFile.ACC_STATIC);
        builder.setExactMethodBody(MethodHandle.VOID_EMPTY);
        return builder.build();
    }

    static Builder builder() {
        return new InitializerElementImpl.Builder();
    }

    interface Builder extends ExactExecutableElement.Builder {
        InitializerElement build();
    }
}
