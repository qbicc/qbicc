package org.qbicc.plugin.serialization;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.object.Section;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.MethodElement;

/**
 * Emit the initial heap and relocation offsets into the object file
 * for org.qbicc.runtime.main.InitialHeap.
 */
public class HeapSerializer implements Consumer<CompilationContext> {
    public static final String heapSymbol = "qbicc_initial_heap_bytes";
    public static final String heapRelocations = "qbicc_initial_heap_relocations";

    public void accept(CompilationContext ctxt) {
        BuildtimeHeap heap = BuildtimeHeap.get(ctxt);

        LoadedTypeDefinition od = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap").load();
        CompoundType heapCT = emitHeapBytes(ctxt, od, heap.getHeapBytes());
        emitGetInitialHeapImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "getInitialHeap".equals(e.getName()))), heapCT);
        CompoundType heapRelocCT = emitHeapRelocations(ctxt, od, heap.getHeapRelocations());
        emitGetInitialHeapRelocationsImpl(ctxt, od.getMethod(od.findMethodIndex(e -> "getInitialHeapRelocations".equals(e.getName()))), heapRelocCT);
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

    private static CompoundType emitHeapRelocations(CompilationContext ctxt, LoadedTypeDefinition od, List<Literal> heapRelocs) {
        Section section = ctxt.getImplicitSection(od);
        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();
        Layout layout = Layout.get(ctxt);

        ArrayType relocsArrayType = ts.getArrayType(ts.getSignedInteger32Type(), heapRelocs.size());
        LoadedTypeDefinition intArrayType = layout.getArrayLoadedTypeDefinition("[I");
        CompoundType intArrayCT = layout.getInstanceLayoutInfo(intArrayType).getCompoundType();
        CompoundType.Member intArrayData = intArrayCT.getMember(2);

        CompoundType.Member content = ts.getCompoundTypeMember(intArrayData.getName(), relocsArrayType, intArrayData.getOffset(), intArrayData.getAlign());
        CompoundType relocCT = ts.getCompoundType(CompoundType.Tag.STRUCT, "qbicc_heap_relocs_type", intArrayCT.getSize() + heapRelocs.size(),
            ts.getPointerAlignment(), () -> List.of(intArrayCT.getMember(0), intArrayCT.getMember(1), content));
        HashMap<CompoundType.Member, Literal> valueMap = new HashMap<>();
        valueMap.put(relocCT.getMember(0), lf.literalOf(intArrayType.getTypeId()));
        valueMap.put(relocCT.getMember(1), lf.literalOf(heapRelocs.size()));
        valueMap.put(relocCT.getMember(2),  lf.literalOf(relocsArrayType, heapRelocs));

        section.addData(null, heapRelocations, lf.literalOf(relocCT, valueMap));
        return relocCT;
    }

    // Implementation of InitialHeap.getInitialHeap()
    private static void emitGetInitialHeapImpl(CompilationContext ctxt, MethodElement meth, CompoundType heapCT) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getMethodBody();

        bb.begin(new BlockLabel());
        bb.return_(bb.bitCast(ctxt.getLiteralFactory().literalOfSymbol(heapSymbol, heapCT.getPointer()),
            ctxt.getTypeSystem().getSignedInteger8Type().getPrimitiveArrayObjectType().getReference()));

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }

    // Implementation of InitialHeap.getInitialHeapRelocations()
    private static void emitGetInitialHeapRelocationsImpl(CompilationContext ctxt, MethodElement meth, CompoundType relocsCT) {
        BasicBlockBuilder bb = ctxt.getBootstrapClassContext().newBasicBlockBuilder(meth);
        MethodBody original = meth.getMethodBody();

        bb.begin(new BlockLabel());
        bb.return_(bb.bitCast(ctxt.getLiteralFactory().literalOfSymbol(heapSymbol, relocsCT.getPointer()),
            ctxt.getTypeSystem().getSignedInteger32Type().getPrimitiveArrayObjectType().getReference()));

        bb.finish();
        meth.replaceMethodBody(MethodBody.of(bb.getFirstBlock(), Schedule.forMethod(bb.getFirstBlock()), original.getThisValue(), original.getParameterValues()));
    }
}
