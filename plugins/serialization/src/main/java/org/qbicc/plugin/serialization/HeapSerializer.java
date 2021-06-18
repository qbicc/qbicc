package org.qbicc.plugin.serialization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
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

    public void accept(CompilationContext ctxt) {
        BuildtimeHeap heap = BuildtimeHeap.get(ctxt);

        LoadedTypeDefinition ih = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap").load();
        Section heapSection = ctxt.getOrAddProgramModule(ih).getOrAddSection(ctxt.IMPLICIT_SECTION_NAME); // TODO: use ctxt.INITIAL_HEAP_SECTION_NAME

        for (Map.Entry<SymbolLiteral, Literal> e : heap.getHeap().entrySet()) {
            Data d = heapSection.addData(null, e.getKey().getName(), e.getValue());
            d.setLinkage(Linkage.EXTERNAL);
            d.setAddrspace(1);
        }
    }
}
