package cc.quarkus.vm.implementation;

import java.util.concurrent.atomic.AtomicReferenceArray;

import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import cc.quarkus.vm.api.JavaObject;

final class FieldContainer {
    private final VerifiedTypeDefinition type;
    final FieldSet fieldSet;
    // todo: autoboxing is really a terrible idea
    final AtomicReferenceArray<Object> objects;

    FieldContainer(VerifiedTypeDefinition type, FieldSet fieldSet) {
        this.type = type;
        this.fieldSet = fieldSet;
        objects = new AtomicReferenceArray<>(fieldSet.getSize());
    }

    JavaObject getObjectFieldPlain(String name) {
        return (JavaObject) objects.getPlain(fieldSet.getIndex(name));
    }

    JavaObject getObjectFieldVolatile(String name) {
        return (JavaObject) objects.get(fieldSet.getIndex(name));
    }

    JavaObject getObjectFieldAcquire(String name) {
        return (JavaObject) objects.getAcquire(fieldSet.getIndex(name));
    }

    long getLongFieldPlain(String name) {
        return ((Number) objects.getPlain(fieldSet.getIndex(name))).longValue();
    }

    long getLongFieldVolatile(String name) {
        return ((Number) objects.get(fieldSet.getIndex(name))).longValue();
    }

    long getLongFieldAcquire(String name) {
        return ((Number) objects.getAcquire(fieldSet.getIndex(name))).longValue();
    }

    int getIntFieldPlain(String name) {
        return ((Number) objects.getPlain(fieldSet.getIndex(name))).intValue();
    }

    int getIntFieldVolatile(String name) {
        return ((Number) objects.get(fieldSet.getIndex(name))).intValue();
    }

    int getIntFieldAcquire(String name) {
        return ((Number) objects.getAcquire(fieldSet.getIndex(name))).intValue();
    }

    void setFieldPlain(String name, JavaObject value) {
        objects.setPlain(fieldSet.getIndex(name), value);
    }

    void setFieldVolatile(String name, JavaObject value) {
        objects.set(fieldSet.getIndex(name), value);
    }

    void setFieldRelease(String name, JavaObject value) {
        objects.setRelease(fieldSet.getIndex(name), value);
    }

    void setFieldPlain(String name, long value) {
        objects.setPlain(fieldSet.getIndex(name), Long.valueOf(value));
    }

    void setFieldVolatile(String name, long value) {
        objects.set(fieldSet.getIndex(name), Long.valueOf(value));
    }

    void setFieldRelease(String name, long value) {
        objects.setRelease(fieldSet.getIndex(name), Long.valueOf(value));
    }

    void setFieldPlain(String name, int value) {
        objects.setPlain(fieldSet.getIndex(name), Integer.valueOf(value));
    }

    void setFieldVolatile(String name, int value) {
        objects.set(fieldSet.getIndex(name), Integer.valueOf(value));
    }

    void setFieldRelease(String name, int value) {
        objects.setRelease(fieldSet.getIndex(name), Integer.valueOf(value));
    }
}
