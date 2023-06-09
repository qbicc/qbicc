package org.qbicc.interpreter.memory;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmObject;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.UnionType;
import org.qbicc.type.UnresolvedType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;

/**
 * Factory methods for producing memory instances.
 */
public final class MemoryFactory {

    private static final String GEN_MEMORY_NAME = "org/qbicc/interpreter/memory/GenMemory";

    private MemoryFactory() {}

    private static final Memory EMPTY = new ByteArrayMemory.LE(new byte[0]);

    /**
     * Get an empty memory.  The memory may have any type (or none) and is not guaranteed to be extendable or copyable.
     *
     * @return an empty memory (not {@code null})
     */
    public static Memory getEmpty() {
        return EMPTY;
    }

    /**
     * Get a read-only zero memory of the given size.
     *
     * @param size the size
     * @return a read-only zero memory (not {@code null})
     */
    public static ZeroMemory getZero(long size) {
        return new ZeroMemory(size);
    }

    public static Memory replicate(Memory first, int nCopies) {
        if (nCopies == 1) {
            return first;
        } else {
            return new VectorMemory(first, nCopies);
        }
    }

    public static Memory compose(Memory... memories) {
        if (memories == null || memories.length == 0) {
            return EMPTY;
        } else if (memories.length == 1) {
            return memories[0];
        } else {
            return new CompositeMemory(memories);
        }
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static ByteArrayMemory wrap(byte[] array, ByteOrder byteOrder) {
        return byteOrder == ByteOrder.BIG_ENDIAN ? new ByteArrayMemory.BE(array) : new ByteArrayMemory.LE(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static ShortArrayMemory wrap(short[] array) {
        return new ShortArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static IntArrayMemory wrap(int[] array) {
        return new IntArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static LongArrayMemory wrap(long[] array) {
        return new LongArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static CharArrayMemory wrap(char[] array) {
        return new CharArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static FloatArrayMemory wrap(float[] array) {
        return new FloatArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static DoubleArrayMemory wrap(double[] array) {
        return new DoubleArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static BooleanArrayMemory wrap(boolean[] array) {
        return new BooleanArrayMemory(array);
    }

    /**
     * Wrap the given array as a memory of the same size and type.
     * The size of a reference is acquired from the given type.
     *
     * @param array the array to wrap (must not be {@code null})
     * @param refType the reference type (must not be {@code null})
     * @return the wrapper memory (must not be {@code null})
     */
    public static ReferenceArrayMemory wrap(VmObject[] array, ReferenceType refType) {
        return new ReferenceArrayMemory(array, refType);
    }

    static final class GenMemoryInfo {
        final Supplier<Memory> producer;

        GenMemoryInfo(Supplier<Memory> producer) {
            this.producer = producer;
        }
    }

    private static final String ABSTRACT_MEMORY_DESC = AbstractMemory.class.descriptorString();
    private static final String CLASS_DESC = Class.class.descriptorString();
    private static final String CTXT_DESC = CompilationContext.class.descriptorString();
    private static final String LOOKUP_DESC = MethodHandles.Lookup.class.descriptorString();
    private static final String MEMORY_DESC = Memory.class.descriptorString();
    private static final String OBJECT_DESC = Object.class.descriptorString();
    private static final String STRING_DESC = String.class.descriptorString();
    private static final String SUPPLIER_DESC = Supplier.class.descriptorString();
    private static final String VAR_HANDLE_DESC = VarHandle.class.descriptorString();

    private static final String EMPTY_TO_ABSTRACT_MEMORY_DESC = "()" + ABSTRACT_MEMORY_DESC;
    private static final String EMPTY_TO_CTXT_DESC = "()" + CTXT_DESC;
    private static final String EMPTY_TO_MEMORY_DESC = "()" + MEMORY_DESC;
    private static final String EMPTY_TO_OBJECT_DESC = "()" + OBJECT_DESC;
    private static final String INT_TO_MEMORY_DESC = "(I)" + MEMORY_DESC;
    private static final String INT_TO_VAR_HANDLE_DESC = "(I)" + VAR_HANDLE_DESC;
    private static final String MEMORY_TO_VOID_DESC = "(" + MEMORY_DESC + ")V";

    private static final Handle PRIMITIVE_CLASS_HANDLE = new Handle(
        Opcodes.H_INVOKESTATIC,
        "java/lang/invoke/ConstantBootstraps",
        "primitiveClass",
        "(" +
            LOOKUP_DESC +
            STRING_DESC +
            CLASS_DESC +
        ")" + CLASS_DESC,
        false
    );

    private static final Handle CLASS_DATA_AT_HANDLE = new Handle(
        Opcodes.H_INVOKESTATIC,
        "java/lang/invoke/MethodHandles",
        "classDataAt",
        "(" +
            LOOKUP_DESC +
            STRING_DESC +
            CLASS_DESC +
            "I" +
        ")" + OBJECT_DESC,
        false
    );

    private static final Handle FIELD_VAR_HANDLE_HANDLE = new Handle(
        Opcodes.H_INVOKESTATIC,
        "java/lang/invoke/ConstantBootstraps",
        "fieldVarHandle",
        "(" +
            LOOKUP_DESC +
            STRING_DESC +
            CLASS_DESC +
            CLASS_DESC +
            CLASS_DESC +
        ")" + VAR_HANDLE_DESC,
        false
    );

    // primitive type class condys
    private static final ConstantDynamic DOUBLE_CLASS_CONSTANT = new ConstantDynamic("D", CLASS_DESC, PRIMITIVE_CLASS_HANDLE);
    private static final ConstantDynamic FLOAT_CLASS_CONSTANT = new ConstantDynamic("F", CLASS_DESC, PRIMITIVE_CLASS_HANDLE);
    private static final ConstantDynamic CHAR_CLASS_CONSTANT = new ConstantDynamic("C", CLASS_DESC, PRIMITIVE_CLASS_HANDLE);
    private static final ConstantDynamic LONG_CLASS_CONSTANT = new ConstantDynamic("J", CLASS_DESC, PRIMITIVE_CLASS_HANDLE);
    private static final ConstantDynamic INT_CLASS_CONSTANT = new ConstantDynamic("I", CLASS_DESC, PRIMITIVE_CLASS_HANDLE);
    private static final ConstantDynamic SHORT_CLASS_CONSTANT = new ConstantDynamic("S", CLASS_DESC, PRIMITIVE_CLASS_HANDLE);
    private static final ConstantDynamic BYTE_CLASS_CONSTANT = new ConstantDynamic("B", CLASS_DESC, PRIMITIVE_CLASS_HANDLE);
    private static final ConstantDynamic BOOLEAN_CLASS_CONSTANT = new ConstantDynamic("Z", CLASS_DESC, PRIMITIVE_CLASS_HANDLE);

    private static final AttachmentKey<Map<StructType, GenMemoryInfo>> MF_CACHE_KEY = new AttachmentKey<>();
    private static final String[] MEM_INTERFACES = { "org/qbicc/interpreter/Memory", "java/lang/Cloneable" };

    public static Supplier<Memory> getMemoryFactory(CompilationContext ctxt, StructType st, boolean upgradeLongs) {
        Map<StructType, GenMemoryInfo> map = ctxt.computeAttachmentIfAbsent(MF_CACHE_KEY, ConcurrentHashMap::new);
        // avoid constructing the lambda instance if possible
        GenMemoryInfo genMemoryInfo = map.get(st);
        if (genMemoryInfo != null) {
            return genMemoryInfo.producer;
        }
        return map.computeIfAbsent(st, ct1 -> makeFactory(ct1, ctxt, upgradeLongs)).producer;
    }

    private static final ConstantDynamic CTXT = new ConstantDynamic(ConstantDescs.DEFAULT_NAME, CTXT_DESC, CLASS_DATA_AT_HANDLE, Integer.valueOf(0));

    private static GenMemoryInfo makeFactory(final StructType st, CompilationContext ctxt, boolean upgradeLongs) {
        // produce class per compound type
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cw.visit(Opcodes.V17, Opcodes.ACC_SUPER, GEN_MEMORY_NAME, null, "org/qbicc/interpreter/memory/VarHandleMemory", MEM_INTERFACES);

        // emit size method
        MethodVisitor smv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getSize", "()J", null, null);
        smv.visitCode();
        smv.visitLdcInsn(Long.valueOf(st.getSize()));
        smv.visitInsn(Opcodes.LRETURN);
        smv.visitMaxs(0, 0);
        smv.visitEnd();

        // mapping of offset to condy for each simple field; the condy creates the VarHandle on demand

        final MutableIntObjectMap<ConstantDynamic> handles = IntObjectMaps.mutable.empty();

        // mapping of member to delegate Memory instance for each complex field

        Map<StructType.Member, String> delegate = null;

        Map<String, String> simpleFields = null;

        for (StructType.Member member : st.getMembers()) {
            int offset = member.getOffset();
            String name = member.getName();
            String fieldName = name + '@' + offset;
            Class<?> fieldClazz;
            int fieldAccess;
            ValueType memberType = member.getType();
            if (memberType.getSize() == 0) {
                continue;
            }
            // complex fields
            if (memberType instanceof ArrayType || memberType instanceof StructType || memberType instanceof UnionType) {
                fieldAccess = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL;
                if (delegate == null) {
                    delegate = new HashMap<>();
                }
                fieldClazz = Memory.class;
                delegate.put(member, fieldName);
            } else {
                // simple fields
                if (simpleFields == null) simpleFields = new LinkedHashMap<>();
                Object fieldTypeArg;
                fieldAccess = Opcodes.ACC_PRIVATE;
                if (memberType instanceof PointerType || upgradeLongs && memberType instanceof IntegerType it && it.getMinBits() == 64) {
                    fieldClazz = Pointer.class;
                    fieldTypeArg = Type.getType(fieldClazz);
                } else if (memberType instanceof IntegerType it) {
                    fieldClazz = switch (it.getMinBits()) {
                        case 8 -> byte.class;
                        case 16 -> it instanceof UnsignedIntegerType ? char.class : short.class;
                        case 32 -> int.class;
                        case 64 -> long.class;
                        default -> throw new IllegalStateException();
                    };
                    fieldTypeArg = switch (it.getMinBits()) {
                        case 8 -> BYTE_CLASS_CONSTANT;
                        case 16 -> it instanceof UnsignedIntegerType ? CHAR_CLASS_CONSTANT : SHORT_CLASS_CONSTANT;
                        case 32 -> INT_CLASS_CONSTANT;
                        case 64 -> LONG_CLASS_CONSTANT;
                        default -> throw new IllegalStateException();
                    };
                } else if (memberType instanceof FloatType ft) {
                    fieldClazz = switch (ft.getMinBits()) {
                        case 32 -> float.class;
                        case 64 -> double.class;
                        default -> throw new IllegalStateException();
                    };
                    fieldTypeArg = switch (ft.getMinBits()) {
                        case 32 -> FLOAT_CLASS_CONSTANT;
                        case 64 -> DOUBLE_CLASS_CONSTANT;
                        default -> throw new IllegalStateException();
                    };
                } else if (memberType instanceof BooleanType) {
                    fieldClazz = boolean.class;
                    fieldTypeArg = BOOLEAN_CLASS_CONSTANT;
                } else if (memberType instanceof TypeIdType) {
                    fieldClazz = ValueType.class;
                    fieldTypeArg = Type.getType(fieldClazz);
                } else if (memberType instanceof ReferenceType) {
                    fieldClazz = VmObject.class;
                    fieldTypeArg = Type.getType(fieldClazz);
                } else if (memberType instanceof UnresolvedType) {
                    fieldClazz = VmObject.class;
                    fieldTypeArg = Type.getType(fieldClazz);
                } else {
                    throw new IllegalStateException("Unknown type");
                }
                ConstantDynamic constant = new ConstantDynamic(fieldName, VAR_HANDLE_DESC, FIELD_VAR_HANDLE_HANDLE,
                    Type.getObjectType(GEN_MEMORY_NAME),
                    fieldTypeArg
                );
                handles.put(offset, constant);
                simpleFields.put(fieldName, fieldClazz.descriptorString());
            }

            // instance field
            FieldVisitor fieldVisitor = cw.visitField(fieldAccess, fieldName, fieldClazz.descriptorString(), null, null);
            fieldVisitor.visitEnd();
        }

        // emit "getHandle" method
        MethodVisitor ghmv = ((ClassVisitor) cw).visitMethod(Opcodes.ACC_PROTECTED, "getHandle", INT_TO_VAR_HANDLE_DESC, null, null);
        ghmv.visitParameter("offset", 0);
        ghmv.visitCode();// emit a switch statement to map offsets to condys
        Label noMatch = new Label();
        int cnt = handles.size();
        int[] keys = new int[cnt];
        Label[] labels = new Label[cnt];
        ConstantDynamic[] constants = new ConstantDynamic[cnt];
        int i = 0;
        RichIterable<IntObjectPair<ConstantDynamic>> iter = handles.keyValuesView();
        ArrayList<IntObjectPair<ConstantDynamic>> list = iter.into(new ArrayList<>());
        list.sort(Comparator.comparingInt(IntObjectPair::getOne));
        for (IntObjectPair<ConstantDynamic> pair : list) {
            keys[i] = pair.getOne();
            labels[i] = new Label();
            constants[i] = pair.getTwo();
            i++;
        }
        ghmv.visitVarInsn(Opcodes.ILOAD, 1); // offset
        ghmv.visitLookupSwitchInsn(noMatch, keys, labels);
        for (i = 0; i < cnt; i++) {
            ghmv.visitLabel(labels[i]);
            ghmv.visitLdcInsn(constants[i]);
            ghmv.visitInsn(Opcodes.ARETURN);
        }
        ghmv.visitLabel(noMatch);
        ghmv.visitVarInsn(Opcodes.ALOAD, 0); // this
        ghmv.visitVarInsn(Opcodes.ILOAD, 1); // offset
        ghmv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/qbicc/interpreter/memory/VarHandleMemory", "getHandle", INT_TO_VAR_HANDLE_DESC, false);
        ghmv.visitInsn(Opcodes.ARETURN);
        ghmv.visitMaxs(0, 0);
        ghmv.visitEnd();

        // emit ctor
        MethodVisitor ctor = cw.visitMethod(0, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/qbicc/interpreter/memory/VarHandleMemory", "<init>", "()V", false);

        // emit "clone" method
        MethodVisitor cmv = cw.visitMethod(Opcodes.ACC_PUBLIC, "clone", EMPTY_TO_MEMORY_DESC, null, null);
        cmv.visitCode();
        if (delegate == null) {
            // shallow clone, cool!
            cmv.visitVarInsn(Opcodes.ALOAD, 0);
            cmv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/qbicc/interpreter/memory/AbstractMemory", "doClone", EMPTY_TO_ABSTRACT_MEMORY_DESC, false);
            cmv.visitInsn(Opcodes.ARETURN);
        } else {
            // deep clone :( emit a copy constructor and call it
            MethodVisitor ccmv = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", MEMORY_TO_VOID_DESC, null, null);
            ccmv.visitParameter("orig", 0);
            ccmv.visitCode();
            // call super
            ccmv.visitVarInsn(Opcodes.ALOAD, 0); // this
            ccmv.visitVarInsn(Opcodes.ALOAD, 1); // this orig
            ccmv.visitTypeInsn(Opcodes.CHECKCAST, GEN_MEMORY_NAME); // this orig'
            ccmv.visitVarInsn(Opcodes.ASTORE, 1); // this
            ccmv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/qbicc/interpreter/memory/VarHandleMemory", "<init>", "()V", false); // --
            // shallow-copy all of the little fields
            if (simpleFields != null) for (Map.Entry<String, String> entry : simpleFields.entrySet()) {
                String fieldName = entry.getKey();
                String fieldDesc = entry.getValue();
                ccmv.visitVarInsn(Opcodes.ALOAD, 0);
                ccmv.visitVarInsn(Opcodes.ALOAD, 1);
                ccmv.visitFieldInsn(Opcodes.GETFIELD, GEN_MEMORY_NAME, fieldName, fieldDesc);
                ccmv.visitFieldInsn(Opcodes.PUTFIELD, GEN_MEMORY_NAME, fieldName, fieldDesc);
            }
            // deep-copy all of the sub-memories
            for (String fieldName : delegate.values()) {
                ccmv.visitVarInsn(Opcodes.ALOAD, 0);
                ccmv.visitVarInsn(Opcodes.ALOAD, 1);
                ccmv.visitFieldInsn(Opcodes.GETFIELD, GEN_MEMORY_NAME, fieldName, MEMORY_DESC);
                ccmv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/qbicc/interpreter/Memory", "clone", EMPTY_TO_MEMORY_DESC, true);
                ccmv.visitFieldInsn(Opcodes.PUTFIELD, GEN_MEMORY_NAME, fieldName, MEMORY_DESC);
            }

            ccmv.visitInsn(Opcodes.RETURN);
            ccmv.visitMaxs(0, 0);
            ccmv.visitEnd();

            cmv.visitTypeInsn(Opcodes.NEW, GEN_MEMORY_NAME);
            cmv.visitInsn(Opcodes.DUP);
            cmv.visitVarInsn(Opcodes.ALOAD, 0);
            cmv.visitMethodInsn(Opcodes.INVOKESPECIAL, GEN_MEMORY_NAME, "<init>", MEMORY_TO_VOID_DESC, false);
            cmv.visitInsn(Opcodes.ARETURN);
        }
        cmv.visitMaxs(0, 0);
        cmv.visitEnd();

        List<Object> attachment = List.of(ctxt);
        // emit "getDelegateMemory" if needed
        if (delegate != null) {
            //java.lang.invoke.MethodHandles.classData
            attachment = new ArrayList<>(delegate.size() + 1);
            attachment.add(ctxt);

            int fi = 1;
            //    protected Memory getDelegateMemory(int offset) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "getDelegateMemory", INT_TO_MEMORY_DESC, null, null);
            mv.visitParameter("offset", 0);
            mv.visitCode();
            // now it's just there, on the stack

            for (Map.Entry<StructType.Member, String> entry : delegate.entrySet()) {
                StructType.Member member = entry.getKey();
                String fieldName = entry.getValue();
                Label outOfRange = new Label();
                // index < min || (min + size) <= index
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitLdcInsn(Integer.valueOf(member.getOffset()));
                mv.visitJumpInsn(Opcodes.IF_ICMPLT, outOfRange);
                mv.visitLdcInsn(Integer.valueOf((int) (member.getOffset() + member.getType().getSize())));
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitJumpInsn(Opcodes.IF_ICMPLE, outOfRange);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, GEN_MEMORY_NAME, fieldName, MEMORY_DESC);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLabel(outOfRange);

                // also insert the init code for the field to the ctor
                Supplier<Memory> factory = () -> {
                    if (member.getOffset() > 0) {
                        // the delegate memory is offset, so use a zero-memory to pad it out
                        return MemoryFactory.compose(MemoryFactory.getZero(member.getOffset()), MemoryFactory.allocate(ctxt, member.getType(), 1, upgradeLongs));
                    } else {
                        return MemoryFactory.allocate(ctxt, member.getType(), 1, upgradeLongs);
                    }
                };
                attachment.add(factory);
                ctor.visitVarInsn(Opcodes.ALOAD, 0); // this
                ctor.visitLdcInsn(new ConstantDynamic(ConstantDescs.DEFAULT_NAME, SUPPLIER_DESC, CLASS_DATA_AT_HANDLE, Integer.valueOf(fi))); // this factory
                ctor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/function/Supplier", "get", EMPTY_TO_OBJECT_DESC, true); // this memory
                ctor.visitTypeInsn(Opcodes.CHECKCAST, "org/qbicc/interpreter/Memory");
                ctor.visitFieldInsn(Opcodes.PUTFIELD, GEN_MEMORY_NAME, fieldName, MEMORY_DESC);
                fi ++;
            }
            // all out of range
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        // implement getCompilationContext() in terms of condy

        MethodVisitor gccmv = cw.visitMethod(Opcodes.ACC_PROTECTED, "getCompilationContext", EMPTY_TO_CTXT_DESC, null, null);
        gccmv.visitCode();
        gccmv.visitLdcInsn(CTXT);
        gccmv.visitInsn(Opcodes.ARETURN);
        gccmv.visitMaxs(0, 0);
        gccmv.visitEnd();

        cw.visitEnd();

        byte[] bytes = cw.toByteArray();
        Class<? extends Memory> clazz;
        MethodHandles.Lookup hiddenClassLookup;
        try {
            hiddenClassLookup = lookup.defineHiddenClassWithClassData(bytes, attachment, false);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected illegal access", e);
        }
        clazz = hiddenClassLookup.lookupClass().asSubclass(Memory.class);
        MethodHandle constructor;
        try {
            constructor = hiddenClassLookup.findConstructor(clazz, MethodType.methodType(void.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unexpected failure finding constructor", e);
        }
        return new GenMemoryInfo(() -> {
            try {
                return (Memory) constructor.invoke();
            } catch (Throwable e) {
                throw new IllegalStateException("Unexpected construction failure", e);
            }
        });
    }

    /**
     * Allocate a strongly typed memory with the given type and count.
     *
     * @param ctxt the compilation context (must not be {@code null})
     * @param type the type (must not be {@code null})
     * @param count the number of sequential elements to allocate
     * @param upgradeLongs {@code true} to upgrade {@code long} members to {@code Pointer} members, or {@code false} to
     *                     leave them as-is
     * @return the memory
     */
    public static Memory allocate(CompilationContext ctxt, ValueType type, long count, boolean upgradeLongs) {
        ByteOrder byteOrder = type.getTypeSystem().getEndianness();
        if (type instanceof VoidType || count == 0) {
            return getEmpty();
        }
        if (type instanceof ArrayType at) {
            long ec = count * at.getElementCount();
            if (ec > (1 << 30)) {
                // todo: we could vector some memories together...
                throw new IllegalArgumentException("too big");
            }
            ValueType et = at.getElementType();
            if (ec == 1) {
                return allocate(ctxt, et, 1, upgradeLongs);
            } else {
                return allocate(ctxt, et, ec, upgradeLongs);
            }
        }
        int intCount = Math.toIntExact(count);
        // vectored
        if (type instanceof StructType st) {
            return getMemoryFactory(ctxt, st, upgradeLongs).get();
        } else if (type instanceof UnionType ut) {
            return MemoryFactory.getZero(ut.getSize());
        } else if (type instanceof IntegerType it) {
            // todo: more compact impls
            return switch (it.getMinBits()) {
                case 8 -> wrap(new byte[intCount], byteOrder);
                case 16 -> it instanceof SignedIntegerType ? wrap(new short[intCount]) : wrap(new char[intCount]);
                case 32 -> wrap(new int[intCount]);
                case 64 -> wrap(new long[intCount]);
                default -> throw new IllegalArgumentException();
            };
        } else if (type instanceof FloatType ft) {
            return switch (ft.getMinBits()) {
                case 32 -> wrap(new float[intCount]);
                case 64 -> wrap(new double[intCount]);
                default -> throw new IllegalArgumentException();
            };
        } else if (type instanceof BooleanType) {
            return wrap(new boolean[intCount]);
        } else {
            // todo: pointers, types
            throw new IllegalArgumentException();
        }
    }
}
