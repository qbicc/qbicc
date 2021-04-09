package org.qbicc.plugin.instanceofcheckcast;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.ValidatedTypeDefinition;

/**
 * Build Cohen's Display for Super types for all classes present in
 * the RTAInfo.
 */
public class SupersDisplayBuilder implements Consumer<CompilationContext> {
    
    @Override
    public void accept(CompilationContext ctxt) {
        // NOTE: CoreIntrinsics.registerOrgQbiccObjectModelIntrinsics depends on the exact order
        //       in which typeIds are assigned to implement intrinsics is_class, is_interface, and is_prim_array.
        //       If any changes are made in the order here, the implementation of those primitives must be updated!

        RTAInfo info = RTAInfo.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        // Starting from java.lang.Object walk down the live class hierarchy and
        // compute supers display that contain just the classes where RTAInfo
        // marks the class as live
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        ValidatedTypeDefinition jlo = jloDef.validate();
        tables.buildSupersDisplay(jlo);
        info.visitLiveSubclassesPreOrder(jlo, tables::buildSupersDisplay);
        // Assign typeIDs to classes
        // [0] Poisioned entry for easier debugging
        // primitives
        // [1] boolean.class
        // [2] byte.class
        // [3] short.class
        // [4] char.class
        // [5] int.class
        // [6] float.class
        // [7] long.class
        // [8] double.class
        // [9] void.class
        tables.reserveTypeIds(10);

        // object
        tables.assignTypeID(jlo);
        // arrays, including reference array
        Layout layout = Layout.get(ctxt);
        // [Object + 1] boolean[].class
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[Z"));
        // [Object + 2] byte[].class
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[B"));
        // [Object + 3] short[].class
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[S"));
        // [Object + 4] char[].class
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[C"));
        // [Object + 5] int[].class
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[I"));
        // [Object + 6] float[].class
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[F"));
        // [Object + 7] long[].class
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[J"));
        // [Object + 8] double[].class
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[D"));
        // [Object + 9] Reference[]
        tables.assignTypeID(layout.getArrayValidatedTypeDefinition("[ref"));

        // subclasses of object
        info.visitLiveSubclassesPreOrder(jlo, tables::assignTypeID);

        // back propagate max subclass typeid
        info.visitLiveSubclassesPostOrder(jlo, tables::assignMaximumSubtypeId);

        // visit all interfaces implemented as determined by the RTAInfo
        info.visitLiveInterfaces(tables::assignInterfaceId);

        tables.updateJLORange(jlo);

        tables.statistics();

        tables.writeTypeIdToClasses();

        // emit the typeid[] into Object's file
        tables.emitTypeIdTable(jlo);
    }
}