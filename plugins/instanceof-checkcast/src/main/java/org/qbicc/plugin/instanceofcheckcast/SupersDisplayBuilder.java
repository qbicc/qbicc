package org.qbicc.plugin.instanceofcheckcast;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.context.ClassContext;
import org.qbicc.type.Primitive;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * Build Cohen's Display for Super types for all classes present in
 * the ReachabilityInfo.
 */
public class SupersDisplayBuilder implements Consumer<CompilationContext> {
    
    @Override
    public void accept(CompilationContext ctxt) {
        // NOTE: CoreIntrinsics.registerOrgQbiccObjectModelIntrinsics depends on the exact order
        //       in which typeIds are assigned to implement intrinsics is_class, is_interface, is_prim_array, and is_primitive.
        //       If any changes are made in the order here, the implementation of those primitives must be updated!

        ReachabilityInfo info = ReachabilityInfo.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        // Starting from java.lang.Object walk down the live class hierarchy and
        // compute supers display that contain just the classes where ReachabilityInfo
        // marks the class as live
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition jloDef = classContext.findDefinedType("java/lang/Object");
        LoadedTypeDefinition jlo = jloDef.load();
        tables.buildSupersDisplay(jlo);
        info.visitReachableSubclassesPreOrder(jlo, tables::buildSupersDisplay);
        CoreClasses coreClasses = CoreClasses.get(ctxt);

        // Assign typeIDs to classes
        // [0 - 8] for void and primitive types
        Primitive.VOID.setTypeId(tables.assignTypeID(coreClasses.getVoidTypeDefinition()));
        Primitive.BOOLEAN.setTypeId(tables.assignTypeID(coreClasses.getBooleanTypeDefinition()));
        Primitive.BYTE.setTypeId(tables.assignTypeID(coreClasses.getByteTypeDefinition()));
        Primitive.SHORT.setTypeId(tables.assignTypeID(coreClasses.getShortTypeDefinition()));
        Primitive.CHAR.setTypeId(tables.assignTypeID(coreClasses.getCharTypeDefinition()));
        Primitive.INT.setTypeId(tables.assignTypeID(coreClasses.getIntTypeDefinition()));
        Primitive.FLOAT.setTypeId(tables.assignTypeID(coreClasses.getFloatTypeDefinition()));
        Primitive.LONG.setTypeId(tables.assignTypeID(coreClasses.getLongTypeDefinition()));
        Primitive.DOUBLE.setTypeId(tables.assignTypeID(coreClasses.getDoubleTypeDefinition()));

        // object
        tables.assignTypeID(jlo);
        // arrays, including reference array
        tables.assignTypeID(coreClasses.getArrayBaseTypeDefinition());
        // [Object + 1] boolean[].class
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[Z"));
        // [Object + 2] byte[].class
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[B"));
        // [Object + 3] short[].class
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[S"));
        // [Object + 4] char[].class
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[C"));
        // [Object + 5] int[].class
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[I"));
        // [Object + 6] float[].class
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[F"));
        // [Object + 7] long[].class
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[J"));
        // [Object + 8] double[].class
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[D"));
        // [Object + 9] Reference[]
        tables.assignTypeID(coreClasses.getArrayLoadedTypeDefinition("[ref"));

        // subclasses of object
        info.visitReachableSubclassesPreOrder(jlo, tables::assignTypeID);

        // back propagate max subclass typeid
        info.visitReachableSubclassesPostOrder(jlo, tables::assignMaximumSubtypeId);

        // visit all interfaces implemented as determined by the ReachabilityInfo
        info.visitReachableInterfaces(tables::assignInterfaceId);

        tables.updateJLORange(jlo);

        tables.statistics();

        tables.writeTypeIdToClasses();

        tables.defineTypeIdStructAndGlobalArray(jlo);
    }
}