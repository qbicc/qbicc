package org.qbicc.plugin.serialization;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.interpreter.VmClass;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.type.ArrayType;
import org.qbicc.type.Primitive;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

/**
 * Constructs and emits an array of java.lang.Class references indexed by typeId.
 */
public class ClassObjectSerializer implements Consumer<CompilationContext> {
    @Override
    public void accept(CompilationContext ctxt) {
        BuildtimeHeap bth = BuildtimeHeap.get(ctxt);

        bth.initializeRootClassArray(SupersDisplayTables.get(ctxt).get_number_of_typeids());

        // Serialize all the root Class instances
        ReachabilityInfo.get(ctxt).visitReachableTypes(ltd -> {
            VmClass vmClass = ltd.getVmClass();
            bth.serializeVmObject(vmClass);
        });
        Primitive.forEach(type -> {
            VmClass vmClass = ctxt.getVm().getPrimitiveClass(type);
            bth.serializeVmObject(vmClass);
        });

        bth.emitRootClassArray();
    }
}
