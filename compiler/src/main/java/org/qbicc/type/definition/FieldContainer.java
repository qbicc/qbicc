package org.qbicc.type.definition;

import org.qbicc.interpreter.VmObject;
import io.smallrye.common.constraint.Assert;

public interface FieldContainer {
    static FieldContainer forInstanceFieldsOf(ValidatedTypeDefinition definition) {
        return new FieldContainerImpl(Assert.checkNotNullParam("definition", definition), false);
    }

    static FieldContainer forStaticFieldsOf(ValidatedTypeDefinition definition) {
        return new FieldContainerImpl(Assert.checkNotNullParam("definition", definition), true);
    }

    FieldSet getFieldSet();

    VmObject getObjectFieldPlain(String name);

    VmObject getObjectFieldVolatile(String name);

    VmObject getObjectFieldAcquire(String name);

    long getLongFieldPlain(String name);

    long getLongFieldVolatile(String name);

    long getLongFieldAcquire(String name);

    int getIntFieldPlain(String name);

    int getIntFieldVolatile(String name);

    int getIntFieldAcquire(String name);

    void setFieldPlain(String name, VmObject value);

    void setFieldVolatile(String name, VmObject value);

    void setFieldRelease(String name, VmObject value);

    void setFieldPlain(String name, long value);

    void setFieldVolatile(String name, long value);

    void setFieldRelease(String name, long value);

    void setFieldPlain(String name, int value);

    void setFieldVolatile(String name, int value);

    void setFieldRelease(String name, int value);
}
