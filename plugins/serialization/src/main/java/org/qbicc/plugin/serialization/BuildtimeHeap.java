package org.qbicc.plugin.serialization;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuildtimeHeap {
    private static final AttachmentKey<BuildtimeHeap> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final List<InitializedField> heapRoots = new ArrayList<>();
    private final Set<LoadedTypeDefinition> usedClasses = new HashSet<>();
    private final Serializer ser;

    private BuildtimeHeap(CompilationContext ctxt) {
        this.ctxt = ctxt;
        this.ser = new Serializer(ctxt);
    }

    public static BuildtimeHeap get(CompilationContext ctxt) {
        BuildtimeHeap heap = ctxt.getAttachment(KEY);
        if (heap == null) {
            heap = new BuildtimeHeap(ctxt);
            BuildtimeHeap appearing = ctxt.putAttachmentIfAbsent(KEY, heap);
            if (appearing != null) {
                heap = appearing;
            }
        }
        return heap;
    }

    public void addStaticField(FieldElement field, Object value) {
        heapRoots.add(new InitializedField(field, value));
    }

    public void serializeHeap() {
        try {
            for (InitializedField initField : heapRoots) {
                ser.writeObject(initField.value, usedClasses);
            }
        } catch (IOException e) {
            ctxt.error(e,"Error serializing build-time heap");
        }
    }

    public byte[] getHeapBytes() {
        try {
            return ser.getBytes();
        } catch (IOException e) {
            ctxt.error(e,"Error serializing build-time heap");
            return new byte[0];
        }
    }

    public List<InitializedField> getHeapRoots() {
        return heapRoots;
    }

    public int getNumberOfObjects() {
        return ser.getNumberOfObjects();
    }

    public Set<LoadedTypeDefinition> getUsedClasses() {
        return this.usedClasses;
    }

    public static class InitializedField {
        final FieldElement field;
        final Object value;

        InitializedField(FieldElement field, Object value) {
            this.field = field;
            this.value = value;
        }
    }
}
