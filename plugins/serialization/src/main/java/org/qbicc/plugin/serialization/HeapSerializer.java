package org.qbicc.plugin.serialization;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.object.Section;
import org.qbicc.plugin.gc.nogc.NoGc;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;

/**
 * This compiler pass accomplishes the following:
 *   1. Generates the program-specific deserialization methods for RuntimeObjectDeserializer
 *   2. Serializes the build-time head created by the interpreter and injects the resulting byte[]
 *      into the executable.
 */
public class HeapSerializer implements Consumer<CompilationContext> {

    private static final String heapSymbol = "qbicc_pickled_heap_bytes";

    public void accept(CompilationContext ctxt) {
        BuildtimeHeap heap = BuildtimeHeap.get(ctxt);

        // TEMP TESTING: Serialize an int[] with known values to a known static field just to get the plumbing worked out
        LoadedTypeDefinition main = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/Main").load();
        FieldElement f = main.findField("watermark");
        heap.addStaticField(f, new int[]{10, 32});
        // END TEMP TESTING

        heap.serializeHeap();

        LoadedTypeDefinition od = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/deserialization/RuntimeObjectDeserializer").load();
        CompoundType heapCT = emitHeapBytes(ctxt, od, heap.getHeapBytes());
        synthesizeRuntimeObjectDeserializerMethods(ctxt, od, heap, heapCT);
    }

    private static CompoundType emitHeapBytes(CompilationContext ctxt, LoadedTypeDefinition od, byte[] heapBytes) {
        Section section = ctxt.getImplicitSection(od);
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();
        Layout layout = Layout.get(ctxt);

        ArrayType rawHeapArrayType = ts.getArrayType(ts.getSignedInteger8Type(), heapBytes.length);
        LoadedTypeDefinition byteArrayType = layout.getArrayLoadedTypeDefinition("[B");
        CompoundType byteArrayCT = layout.getInstanceLayoutInfo(byteArrayType).getCompoundType();
        CompoundType.Member byteArrayData = byteArrayCT.getMember(2);

        CompoundType.Member content = ts.getCompoundTypeMember(byteArrayData.getName(), rawHeapArrayType, byteArrayData.getOffset(), byteArrayData.getAlign());
        CompoundType heapCT = ts.getCompoundType(CompoundType.Tag.STRUCT, "qbicc_heap_bytes_type", byteArrayCT.getSize() + heapBytes.length,
            ts.getPointerAlignment(), () -> List.of(byteArrayCT.getMember(0), byteArrayCT.getMember(1), content));
        HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
        valueMap.put(heapCT.getMember(0), lf.literalOf(byteArrayType.getTypeId()));
        valueMap.put(heapCT.getMember(1), lf.literalOf(heapBytes.length));
        valueMap.put(heapCT.getMember(2),  lf.literalOf(rawHeapArrayType, heapBytes));

        section.addData(null, heapSymbol, lf.literalOf(heapCT, valueMap));
        return heapCT;
    }


    private static void synthesizeRuntimeObjectDeserializerMethods(CompilationContext ctxt, LoadedTypeDefinition od, BuildtimeHeap heap, CompoundType heapCT) {
        Set<LoadedTypeDefinition> usedClasses = heap.getUsedClasses();
        // TODO: TEMPORARY: Until we actually serialize a real heap, seed usedClasses with 3 entries to test codegen.
        LoadedTypeDefinition[] classes = usedClasses.toArray(new LoadedTypeDefinition[usedClasses.size() + 4]); // TODO: +4 is temporary
        classes[classes.length - 4] = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/deserialization/Deserializer").load();
        classes[classes.length - 3] = od;
        classes[classes.length - 2] = od.getSuperClass();
        classes[classes.length - 1] = od.getSuperClass().getSuperClass();
        Arrays.sort(classes, Comparator.comparingInt(LoadedTypeDefinition::getTypeId));

        allocateClassImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "allocateClass".equals(e.getName()))), classes);
        allocatePrimitiveArrayImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "allocatePrimitiveArray".equals(e.getName()))));
        allocateReferenceArrayImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "allocateReferenceArray".equals(e.getName()))));
        createStringImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "createString".equals(e.getName()))));
        deserializeInstanceFieldsImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "deserializeInstanceFields".equals(e.getName()))), classes);
        deserializePrimitiveArrayDataImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "deserializePrimitiveArrayData".equals(e.getName()))));
        deserializeHeapImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "deserializeHeap".equals(e.getName()))), heap);
        getNumberOfPickledObjectsImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "getNumberOfPickledObjects".equals(e.getName()))), heap);
        getPickledHeapImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "getPickledHeap".equals(e.getName()))), heapCT);
    }


    // Implementation of RuntimeObjectDeserializer.allocateClass(int, bool, bool)
    private static void allocateClassImpl(CompilationContext ctxt, MethodElement meth, LoadedTypeDefinition[] classes) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();
        Layout layout = Layout.get(ctxt);

        int numCases = classes.length;
        int[] typeIds = new int[numCases];
        BlockLabel[] targets = new BlockLabel[numCases];
        BlockLabel errorBlock = new BlockLabel();
        BlockLabel merge = new BlockLabel();
        PhiValue sizeInBytes = bb.phi(ctxt.getTypeSystem().getSignedInteger64Type(), merge);
        for (int i=0; i<numCases; i++) {
            targets[i] = new BlockLabel();
            typeIds[i] = classes[i].getTypeId();
        }

        bb.begin(new BlockLabel());
        bb.switch_(original.getParameterValue(0), typeIds, targets, errorBlock);

        for (int i=0; i<numCases; i++) {
            long size = layout.getInstanceLayoutInfo(classes[i]).getCompoundType().getSize();
            bb.begin(targets[i]);
            bb.goto_(merge);
            sizeInBytes.setValueForBlock(ctxt, meth, targets[i], ctxt.getLiteralFactory().literalOf(size));
        }

        bb.begin(errorBlock);
        try {
            bb.invokeStatic(ctxt.getVMHelperMethod("raiseHeapDeserializationError"), List.of());
            bb.unreachable();
        } catch (BlockEarlyTermination ignored) {
            // continue
        }

        bb.begin(merge);
        NoGc noGc = NoGc.get(ctxt);
        LoadedTypeDefinition jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        Value ptrValue =  bb.invokeValueStatic(noGc.getAllocateMethod(), List.of(sizeInBytes, ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getPointerAlignment())));
        ptrValue = bb.invokeValueStatic(noGc.getZeroMethod(), List.of(ptrValue, sizeInBytes));
        Value oop = bb.valueConvert(ptrValue, jlo.getType().getReference());
        bb.store(bb.instanceFieldOf(bb.referenceHandle(oop), layout.getObjectTypeIdField()), original.getParameterValue(0), MemoryAtomicityMode.UNORDERED);
        bb.return_(oop);
        bb.finish();

        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }

    // Implementation of RuntimeObjectDeserializer.allocatePrimitiveArray(int, int, bool, bool)
    private static void allocatePrimitiveArrayImpl(CompilationContext ctxt, MethodElement meth) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();

        PrimitiveArrayObjectType[] primArrays = new PrimitiveArrayObjectType[8];
        primArrays[0] = ctxt.getTypeSystem().getBooleanType().getPrimitiveArrayObjectType();
        primArrays[1] = ctxt.getTypeSystem().getSignedInteger8Type().getPrimitiveArrayObjectType();
        primArrays[2] = ctxt.getTypeSystem().getSignedInteger16Type().getPrimitiveArrayObjectType();
        primArrays[3] = ctxt.getTypeSystem().getUnsignedInteger16Type().getPrimitiveArrayObjectType();
        primArrays[4] = ctxt.getTypeSystem().getSignedInteger32Type().getPrimitiveArrayObjectType();
        primArrays[5] = ctxt.getTypeSystem().getFloat32Type().getPrimitiveArrayObjectType();
        primArrays[6] = ctxt.getTypeSystem().getSignedInteger64Type().getPrimitiveArrayObjectType();
        primArrays[7] = ctxt.getTypeSystem().getFloat64Type().getPrimitiveArrayObjectType();
        int[] typeIds = new int[8];
        BlockLabel[] targets = new BlockLabel[8];
        BlockLabel errorBlock = new BlockLabel();
        for (int i=0; i<8; i++) {
            targets[i] = new BlockLabel();
            typeIds[i] = i + 10; // UGH.  Yet another place we depend on the ordering of prim array typeIds!
        }

        bb.begin(new BlockLabel());
        bb.switch_(original.getParameterValue(0), typeIds, targets, errorBlock);

        for (int i=0; i<8; i++) {
            try {
                bb.begin(targets[i]);
                // TODO: We could consider adding a flag to newArray to disable the generation
                //       of the call to NoGcHelpers.clear because we immediately deserialize the elements.
                bb.return_(bb.newArray(primArrays[i], original.getParameterValue(1)));
            } catch (BlockEarlyTermination ignored) {
                // continue to next case
            }
        }

        bb.begin(errorBlock);
        try {
            bb.invokeStatic(ctxt.getVMHelperMethod("raiseHeapDeserializationError"), List.of());
            bb.unreachable();
        } catch (BlockEarlyTermination ignored) {
            // continue
        }

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }


    // Implementation of RuntimeObjectDeserializer.allocateReferenceArray(int, int, bool, bool)
    private static void allocateReferenceArrayImpl(CompilationContext ctxt, MethodElement meth) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();
        LoadedTypeDefinition jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load();
        Layout layout = Layout.get(ctxt);

        bb.begin(new BlockLabel());
        // We don't know the elementTypeId at compile time, so allocate an Object[] of the right length and then whack its elementTypeId.
        Value array = bb.newArray(jlo.getType().getReferenceArrayObject(), original.getParameterValue(1)); // Can't elide the clear inside of new; might trigger a GC when deserializing elements
        ValueHandle arrayHandle = bb.referenceHandle(array);
        bb.store(bb.instanceFieldOf(arrayHandle, layout.getRefArrayElementTypeIdField()), original.getParameterValue(0), MemoryAtomicityMode.UNORDERED);
        bb.return_(array);

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }


    // Implementation of RuntimeObjectDeserializer.createString(byte[], byte, bool, bool)
    private static void createStringImpl(CompilationContext ctxt, MethodElement meth) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();

        bb.begin(new BlockLabel());
        LoadedTypeDefinition jls = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load();
        Value str = bb.new_(jls.getClassType());
        bb.store(bb.instanceFieldOf(bb.referenceHandle(str), jls.findField("value")), original.getParameterValue(0), MemoryAtomicityMode.UNORDERED);
        bb.store(bb.instanceFieldOf(bb.referenceHandle(str), jls.findField("coder")), original.getParameterValue(1), MemoryAtomicityMode.UNORDERED);
        bb.return_(str);

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }

    // Implementation of RuntimeObjectDeserializer.deserializeInstanceFields(int, Object, Deserializer)
    private static void deserializeInstanceFieldsImpl(CompilationContext ctxt, MethodElement meth, LoadedTypeDefinition[] classes) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();
        LoadedTypeDefinition deser = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/deserialization/Deserializer").load();

        int numCases = classes.length;
        int[] typeIds = new int[numCases];
        BlockLabel[] targets = new BlockLabel[numCases];
        BlockLabel errorBlock = new BlockLabel();
        for (int i=0; i<numCases; i++) {
            targets[i] = new BlockLabel();
            typeIds[i] = classes[i].getTypeId();
        }

        bb.begin(new BlockLabel());
        bb.switch_(original.getParameterValue(0), typeIds, targets, errorBlock);

        for (int i=0; i<numCases; i++) {
            try {
                bb.begin(targets[i]);
                LoadedTypeDefinition curType = classes[i];
                if (curType.hasSuperClass()) {
                    bb.invokeInstance(DispatchInvocation.Kind.VIRTUAL, original.getThisValue(), meth,
                        List.of(ctxt.getLiteralFactory().literalOf(curType.getSuperClass().getTypeId()), original.getParameterValue(1), original.getParameterValue(2)));

                    ValueHandle baseObj = bb.referenceHandle(bb.bitCast(original.getParameterValue(1), curType.getClassType().getReference()));
                    curType.eachField(f -> {
                        if (!f.isStatic()) {
                            MethodElement reader;
                            if (f.getTypeDescriptor().equals((BaseTypeDescriptor.Z))) {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readBoolean")));
                            } else if (f.getTypeDescriptor().equals((BaseTypeDescriptor.B))) {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readBytes")));
                            } else if (f.getTypeDescriptor().equals((BaseTypeDescriptor.C))) {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readChar")));
                            } else if (f.getTypeDescriptor().equals((BaseTypeDescriptor.S))) {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readShort")));
                            } else if (f.getTypeDescriptor().equals((BaseTypeDescriptor.I))) {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readInt")));
                            } else if (f.getTypeDescriptor().equals((BaseTypeDescriptor.F))) {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readFloat")));
                            } else if (f.getTypeDescriptor().equals((BaseTypeDescriptor.J))) {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readLong")));
                            } else if (f.getTypeDescriptor().equals((BaseTypeDescriptor.D))) {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readDouble")));
                            } else {
                                reader = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readObject")));
                            }
                            bb.store(bb.instanceFieldOf(baseObj, f),
                                bb.invokeValueInstance(DispatchInvocation.Kind.VIRTUAL, original.getParameterValue(2), reader, List.of()),
                                MemoryAtomicityMode.UNORDERED);
                        }
                    });
                }
                bb.return_();
            } catch (BlockEarlyTermination ignored) {
                // continue to next case
            }
        }

        try {
            bb.begin(errorBlock);
            bb.invokeStatic(ctxt.getVMHelperMethod("raiseHeapDeserializationError"), List.of());
            bb.unreachable();
        } catch (BlockEarlyTermination ignored) {
            // continue
        }

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }

    // Implementation of RuntimeObjectDeserializer.deserializePrimitiveArrayData(int, int, Object, Deserializer)
    private static void deserializePrimitiveArrayDataImpl(CompilationContext ctxt, MethodElement meth) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();
        LiteralFactory lf = ctxt.getLiteralFactory();

        LoadedTypeDefinition deser = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/deserialization/Deserializer").load();
        MethodElement[] readers = new MethodElement[8];
        readers[0] = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readBoolean")));
        readers[1] = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readByte")));
        readers[2] = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readShort")));
        readers[3] = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readChar")));
        readers[4] = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readInt")));
        readers[5] = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readFloat")));
        readers[6] = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readLong")));
        readers[7] = deser.getMethod(deser.findMethodIndex(e -> e.getName().equals("readDouble")));
        int[] typeIds = new int[8];
        BlockLabel[] targets = new BlockLabel[8];
        BlockLabel errorBlock = new BlockLabel();
        for (int i=0; i<8; i++) {
            targets[i] = new BlockLabel();
            typeIds[i] = i + 10; // UGH.  Yet another place we depend on the ordering of prim array typeIds!
        }

        bb.begin(new BlockLabel());
        bb.switch_(original.getParameterValue(0), typeIds, targets, errorBlock);

        for (int i=0; i<8; i++) {
            try {
                Value length = original.getParameterValue(1);
                ReferenceType arrayType = ((WordType) readers[i].getType().getReturnType()).getPrimitiveArrayObjectType().getReference();
                ValueHandle arrayHandle = bb.referenceHandle(bb.bitCast(original.getParameterValue(2), arrayType));
                bb.begin(targets[i]);

                // generate a loop to deserialize the elements one at a time
                // TODO: Once we serialize the heap using platform endianess, this loop could be replaced by a straight memcopy + an adjustment of the deser cursor.
                BlockLabel loop = new BlockLabel();
                BasicBlock initial = bb.goto_(loop);
                bb.begin(loop);
                PhiValue phi = bb.phi(length.getType(), loop);
                BlockLabel exit = new BlockLabel();
                BlockLabel resume = new BlockLabel();

                bb.if_(bb.isEq(phi, length), exit, resume);
                try {
                    bb.begin(resume);
                    phi.setValueForBlock(ctxt, meth, initial, lf.literalOf(0));
                    bb.store(bb.elementOf(arrayHandle, phi),
                        bb.invokeValueInstance(DispatchInvocation.Kind.VIRTUAL, original.getParameterValue(3), readers[i], List.of()),
                        MemoryAtomicityMode.UNORDERED);
                    BasicBlock loopExit = bb.goto_(loop);
                    phi.setValueForBlock(ctxt, meth, loopExit, bb.add(phi, lf.literalOf(1)));
                } catch (BlockEarlyTermination ignored) {
                    // continue
                }
                bb.begin(exit);
                bb.return_();
            } catch (BlockEarlyTermination ignored) {
                // continue to next case
            }
        }

        try {
            bb.begin(errorBlock);
            bb.invokeStatic(ctxt.getVMHelperMethod("raiseHeapDeserializationError"), List.of());
            bb.unreachable();
        } catch (BlockEarlyTermination ignored) {
            //continue
        }

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }

    // Implementation of RuntimeObjectDeserializer.deserializeHeap(Deserializer)
    private static void deserializeHeapImpl(CompilationContext ctxt, MethodElement meth, BuildtimeHeap heap) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();
        LoadedTypeDefinition deserializerType = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/deserialization/Deserializer").load();
        MethodElement readObject = deserializerType.getMethod(deserializerType.findMethodIndex(e -> "readObject".equals(e.getName())));

        bb.begin(new BlockLabel());
        for (BuildtimeHeap.InitializedField initField : heap.getHeapRoots()) {
            bb.store(bb.staticField(initField.field),
                bb.invokeValueInstance(DispatchInvocation.Kind.VIRTUAL, original.getParameterValue(0), readObject, List.of()),
                MemoryAtomicityMode.UNORDERED);
        }
        bb.return_();

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }

    // Implementation of RuntimeObjectDeserializer.getNumberOfPickledObjects()
    private static void getNumberOfPickledObjectsImpl(CompilationContext ctxt, MethodElement meth, BuildtimeHeap heap) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();

        bb.begin(new BlockLabel());
        bb.return_(ctxt.getLiteralFactory().literalOf(heap.getNumberOfObjects()));

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }

    // Implementation of RuntimeObjectDeserializer.getPickledHeap()
    private static void getPickledHeapImpl(CompilationContext ctxt, MethodElement meth, CompoundType heapCT) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getOrCreateMethodBody();

        bb.begin(new BlockLabel());
        bb.return_(bb.bitCast(ctxt.getLiteralFactory().literalOfSymbol(heapSymbol, heapCT.getPointer()),
            ctxt.getTypeSystem().getSignedInteger8Type().getPrimitiveArrayObjectType().getReference()));

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }
}
