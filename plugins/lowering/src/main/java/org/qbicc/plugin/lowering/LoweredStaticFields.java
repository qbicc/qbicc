package org.qbicc.plugin.lowering;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.object.Data;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.plugin.constants.Constants;
import org.qbicc.type.BooleanType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;

public class LoweredStaticFields {
    public static final String GLOBAL_REFERENCES = "QBICC_GLOBALS";

    private static final AttachmentKey<LoweredStaticFields> KEY = new AttachmentKey<>();

    private final Map<FieldElement, GlobalVariableElement> globals = new ConcurrentHashMap<>();

    private LoweredStaticFields() {}

    public static LoweredStaticFields get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, LoweredStaticFields::new);
    }

    public GlobalVariableElement getGlobalForField(FieldElement fieldElement) {
        GlobalVariableElement global = globals.get(fieldElement);
        if (global != null) {
            return global;
        }
        DefinedTypeDefinition enclosingType = fieldElement.getEnclosingType();
        ClassContext classContext = enclosingType.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        ValueType fieldType = fieldElement.getType();
        String itemName = "static-" + enclosingType.getInternalName().replace('/', '.') + "-" + fieldElement.getName() + "-" + fieldElement.getIndex();
        final GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName(itemName);
        final ValueType varType = fieldType instanceof BooleanType ? ctxt.getTypeSystem().getUnsignedInteger8Type() : fieldType;
        builder.setType(varType);
        builder.setDescriptor(fieldElement.getTypeDescriptor());
        builder.setSignature(fieldElement.getTypeSignature());
        builder.setModifiers(fieldElement.getModifiers());
        builder.setEnclosingType(enclosingType);
        final String sectionName;
        if (fieldElement.getType() instanceof ReferenceType) {
            sectionName = GLOBAL_REFERENCES;
        } else {
            sectionName = CompilationContext.IMPLICIT_SECTION_NAME;
        }
        builder.setSection(sectionName);
        global = builder.build();
        GlobalVariableElement appearing = globals.putIfAbsent(fieldElement, global);
        if (appearing != null) {
            return appearing;
        }
        Section section = ctxt.getOrAddProgramModule(enclosingType).getOrAddSection(sectionName);
        Value initialValue = fieldElement.getInitialValue();
        Linkage linkage = Linkage.EXTERNAL;
        if (fieldType instanceof ReferenceType) {
            // Reference-typed field values must always be deserialized.
            linkage = Linkage.COMMON;
            initialValue = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(varType);
        } else {
            if (initialValue == null || initialValue instanceof ZeroInitializerLiteral) {
                initialValue = Constants.get(ctxt).getConstantValue(fieldElement);
                if (initialValue == null || initialValue instanceof ZeroInitializerLiteral) {
                    linkage = Linkage.COMMON;
                    initialValue = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(varType);
                }
            }
        }
        if (initialValue.getType() instanceof BooleanType) {
            assert varType instanceof IntegerType;
            // widen the initial value
            if (initialValue instanceof BooleanLiteral) {
                initialValue = ctxt.getLiteralFactory().literalOf((IntegerType) varType, ((BooleanLiteral) initialValue).booleanValue() ? 1 : 0);
            } else if (initialValue instanceof ZeroInitializerLiteral) {
                initialValue = ctxt.getLiteralFactory().literalOf((IntegerType) varType, 0);
            } else {
                throw new IllegalArgumentException("Cannot initialize boolean field");
            }
        }
        final Data data = section.addData(fieldElement, itemName, initialValue);
        data.setLinkage(linkage);
        data.setDsoLocal();
        return global;
    }
}
