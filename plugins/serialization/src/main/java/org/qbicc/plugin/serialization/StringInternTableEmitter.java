package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
import org.qbicc.object.ModuleSection;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;

public class StringInternTableEmitter implements Consumer<CompilationContext> {
    @Override
    public void accept(CompilationContext ctxt) {
        BuildtimeHeap bth = BuildtimeHeap.get(ctxt);
        ArrayList<VmString> used = new ArrayList<>();
        ctxt.getVm().forEachInternedString(vs -> {
            if (bth.containsObject(vs)) {
                used.add(vs);
            }
        });

        // Sort used so String$_native.intern() can use Arrays.binarySearch for runtime lookup
        used.sort(Comparator.comparing(VmString::getContent));

        // Construct and serialize the VmReferenceArray of interned VmStrings
        VmClass jls = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load().getVmClass();
        VmReferenceArray internedStrings = ctxt.getVm().newArrayOf(jls, used.toArray(new VmObject[used.size()]));
        bth.serializeVmObject(internedStrings);

        // Initialize InitialHeap.internedStrings to refer to it
        LoadedTypeDefinition ih = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/main/InitialHeap").load();
        FieldElement field = ih.findField("internedStrings");
        ModuleSection section = ctxt.getImplicitSection(ih);
        Literal theTable = bth.referToSerializedVmObject(internedStrings, internedStrings.getObjectType().getReference(), section.getProgramModule());
        String name = ih.getInternalName().replace('/', '.') + "." + field.getName();
        Data d = section.addData(null, name, theTable);
        d.setLinkage(Linkage.EXTERNAL);
        d.setDsoLocal();
    }
}
