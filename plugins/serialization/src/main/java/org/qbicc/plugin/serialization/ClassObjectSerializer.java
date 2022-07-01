package org.qbicc.plugin.serialization;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmClass;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.type.Primitive;

/**
 * Serializes all reachable Class instances to a flat array indexed by typeId.
 *
 * Also builds and serializes data structures that allow serialized Class instances
 * to be efficiently found at runtime by (classloader, name). These support Class.forName,
 * ClassLoader.findLoadedClass0, and ClassLoader.findBootstrapClass.
 */
public class ClassObjectSerializer implements Consumer<CompilationContext> {
    @Override
    public void accept(CompilationContext ctxt) {
        BuildtimeHeap bth = BuildtimeHeap.get(ctxt);

        bth.initializeRootClassArray(SupersDisplayTables.get(ctxt).get_number_of_typeids());

        ArrayList<VmClass> reachable = new ArrayList<>();

        // Serialize all the root Class instances
        ReachabilityInfo.get(ctxt).visitReachableTypes(ltd -> {
            VmClass vmClass = ltd.getVmClass();
            bth.serializeVmObject(vmClass, false);
            reachable.add(vmClass);
        });
        Primitive.forEach(type -> {
            VmClass vmClass = ctxt.getVm().getPrimitiveClass(type);
            bth.serializeVmObject(vmClass, false);
        });

        bth.emitRootClassArray();
        bth.emitRootClassDictionaries(reachable);
    }
}
