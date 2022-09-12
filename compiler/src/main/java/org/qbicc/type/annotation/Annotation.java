package org.qbicc.type.annotation;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.classfile.ConstantPool;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 * An annotation.
 */
public final class Annotation extends AnnotationValue {
    public static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    private final ClassTypeDescriptor descriptor;
    private final Map<String, AnnotationValue> values;

    private Annotation(final ClassTypeDescriptor descriptor, final Map<String, AnnotationValue> values) {
        this.descriptor = descriptor;
        this.values = values;
    }

    public ClassTypeDescriptor getDescriptor() {
        return descriptor;
    }

    public AnnotationValue getValue(String name) {
        return values.get(name);
    }

    public AnnotationValue getValue(String name, AnnotationValue defaultVal) {
        return values.getOrDefault(name, defaultVal);
    }

    public Set<String> getNames() {
        return values.keySet();
    }

    public Kind getKind() {
        return Kind.ANNOTATION;
    }

    /**
     * Write this annotation's byte representation to the given output stream, populating the synthetic constant
     * pool as it goes. Accesses to the constant pool are NOT synchronized.
     *
     * @param os the output stream (must not be {@code null})
     * @param cp the constant pool (must not be {@code null})
     */
    public void deparseTo(ByteArrayOutputStream os, ConstantPool cp) {
        int typeId = cp.getOrAddUtf8Constant(descriptor.toString());
        writeShort(os, typeId);
        int size = values.size();
        writeShort(os, size);
        for (Map.Entry<String, AnnotationValue> entry : values.entrySet()) {
            writeShort(os, cp.getOrAddUtf8Constant(entry.getKey()));
            entry.getValue().deparseValueTo(os, cp);
        }
    }

    @Override
    public void deparseValueTo(ByteArrayOutputStream os, ConstantPool cp) {
        os.write('@');
        deparseTo(os, cp);
    }

    static void writeShort(final ByteArrayOutputStream os, final int val) {
        // big-endian
        os.write(val >>> 8);
        os.write(val);
    }

    public static Annotation parse(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
        int typeIndex = nextShort(buf);
        ClassTypeDescriptor typeDescriptor = ClassTypeDescriptor.parse(classContext, classFile.getUtf8ConstantAsBuffer(typeIndex));
        int cnt = nextShort(buf);
        final Map<String, AnnotationValue> values = new HashMap<>(cnt);
        for (int i = 0; i < cnt; i ++) {
            int idx = nextShort(buf);
            String name = classFile.getUtf8Constant(idx);
            values.put(name, AnnotationValue.parse(classFile, classContext, buf));
        }
        return new Annotation(typeDescriptor, values);
    }

    public static Annotation synthesize(ClassTypeDescriptor descriptor) {
        return synthesize(descriptor, Map.of());
    }

    public static Annotation synthesize(ClassTypeDescriptor descriptor, final Map<String, AnnotationValue> values) {
        return new Annotation(descriptor, values);
    }

    public static List<Annotation> parseList(final ClassFile classFile, final ClassContext classContext, final ByteBuffer buf) {
        int ac = buf.getShort() & 0xffff;
        Annotation[] annotations = new Annotation[ac];
        for (int j = 0; j < ac; j ++) {
            annotations[j] = parse(classFile, classContext, buf);
        }
        return List.of(annotations);
    }
}
