package org.qbicc.type.definition;

import org.qbicc.interpreter.VmObject;

import java.util.concurrent.atomic.AtomicReferenceArray;

final class FieldContainerImpl implements FieldContainer {
    private final LoadedTypeDefinition type;
    final FieldSet fieldSet;
    // todo: autoboxing is really a terrible idea
    final AtomicReferenceArray<Object> objects;

    FieldContainerImpl(LoadedTypeDefinition type, final boolean statics) {
        this.type = type;
        if (statics) {
            this.fieldSet = type.getStaticFieldSet();
        } else {
            this.fieldSet = type.getInstanceFieldSet();
        }
        objects = new AtomicReferenceArray<>(this.fieldSet.getSize());
    }

    public FieldSet getFieldSet() {
        return fieldSet;
    }

    public VmObject getObjectFieldPlain(String name) {
        return (VmObject) objects.getPlain(fieldSet.getIndex(name));
    }

    public VmObject getObjectFieldVolatile(String name) {
        return (VmObject) objects.get(fieldSet.getIndex(name));
    }

    public VmObject getObjectFieldAcquire(String name) {
        return (VmObject) objects.getAcquire(fieldSet.getIndex(name));
    }

    public long getLongFieldPlain(String name) {
        return ((Number) objects.getPlain(fieldSet.getIndex(name))).longValue();
    }

    public long getLongFieldVolatile(String name) {
        return ((Number) objects.get(fieldSet.getIndex(name))).longValue();
    }

    public long getLongFieldAcquire(String name) {
        return ((Number) objects.getAcquire(fieldSet.getIndex(name))).longValue();
    }

    public int getIntFieldPlain(String name) {
        return ((Number) objects.getPlain(fieldSet.getIndex(name))).intValue();
    }

    public int getIntFieldVolatile(String name) {
        return ((Number) objects.get(fieldSet.getIndex(name))).intValue();
    }

    public int getIntFieldAcquire(String name) {
        return ((Number) objects.getAcquire(fieldSet.getIndex(name))).intValue();
    }

    public void setFieldPlain(String name, VmObject value) {
        objects.setPlain(fieldSet.getIndex(name), value);
    }

    public void setFieldVolatile(String name, VmObject value) {
        objects.set(fieldSet.getIndex(name), value);
    }

    public void setFieldRelease(String name, VmObject value) {
        objects.setRelease(fieldSet.getIndex(name), value);
    }

    public void setFieldPlain(String name, long value) {
        objects.setPlain(fieldSet.getIndex(name), Long.valueOf(value));
    }

    public void setFieldVolatile(String name, long value) {
        objects.set(fieldSet.getIndex(name), Long.valueOf(value));
    }

    public void setFieldRelease(String name, long value) {
        objects.setRelease(fieldSet.getIndex(name), Long.valueOf(value));
    }

    public void setFieldPlain(String name, int value) {
        objects.setPlain(fieldSet.getIndex(name), Integer.valueOf(value));
    }

    public void setFieldVolatile(String name, int value) {
        objects.set(fieldSet.getIndex(name), Integer.valueOf(value));
    }

    public void setFieldRelease(String name, int value) {
        objects.setRelease(fieldSet.getIndex(name), Integer.valueOf(value));
    }
}
