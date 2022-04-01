package org.qbicc.plugin.serialization;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.object.DataDeclaration;
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
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        ReachabilityInfo reachabilityInfo = ReachabilityInfo.get(ctxt);
        LoadedTypeDefinition jlc = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        ModuleSection section = ctxt.getImplicitSection(jlc);
        ProgramModule programModule = section.getProgramModule();
        ReferenceType jlcRef = jlc.getObjectType().getReference();
        ArrayType rootArrayType = ctxt.getTypeSystem().getArrayType(jlcRef, tables.get_number_of_typeids());

        // create the GlobalVariable for shared access to the Class array
        ArrayTypeDescriptor desc = ArrayTypeDescriptor.of(ctxt.getBootstrapClassContext(), jlc.getDescriptor());
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder("qbicc_jlc_lookup_table", desc);
        builder.setType(rootArrayType);
        builder.setEnclosingType(jlc);
        builder.setSignature(TypeSignature.synthesize(ctxt.getBootstrapClassContext(), desc));
        GlobalVariableElement classArrayGlobal = builder.build();
        bth.setClassArrayGlobal(classArrayGlobal);

        // initialize the Class array by serializing java.lang.Class instances for all reachable types and primitive types
        Literal[] rootTable = new Literal[tables.get_number_of_typeids()];
        Arrays.fill(rootTable, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(jlcRef));
        reachabilityInfo.visitReachableTypes(ltd -> {
            ProgramObjectLiteral cls = bth.serializeClassObject(ltd);
            DataDeclaration decl = programModule.declareData(cls.getProgramObject());
            decl.setAddrspace(1);
            ProgramObjectLiteral refToClass = ctxt.getLiteralFactory().literalOf(decl);
            rootTable[ltd.getTypeId()] = ctxt.getLiteralFactory().bitcastLiteral(refToClass, jlcRef);
        });

        Primitive.forEach(type -> {
            ProgramObjectLiteral cls = bth.serializeClassObject(type);
            if (cls != null) {
                DataDeclaration decl = programModule.declareData(cls.getProgramObject());
                decl.setAddrspace(1);
                ProgramObjectLiteral refToClass = ctxt.getLiteralFactory().literalOf(decl);
                rootTable[type.getTypeId()] = ctxt.getLiteralFactory().bitcastLiteral(refToClass, jlcRef);
            }
        });

        // Add the final data value for the constructed Class array
        section.addData(null, classArrayGlobal.getName(), ctxt.getLiteralFactory().literalOf(rootArrayType, List.of(rootTable)));
    }
}
