package org.qbicc.plugin.lowering;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.object.Linkage;
import org.qbicc.object.Section;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

public class LoweredStaticFields {
    private static final AttachmentKey<LoweredStaticFields> KEY = new AttachmentKey<>();

    private final Map<FieldElement, SymbolLiteral> staticFields = new ConcurrentHashMap<>();

    private LoweredStaticFields() {}

    public static LoweredStaticFields get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, LoweredStaticFields::new);
    }

    public SymbolLiteral getSymbolForField(FieldElement fieldElement) {
        SymbolLiteral symbol = staticFields.get(fieldElement);
        if (symbol != null) {
            return symbol;
        }
        DefinedTypeDefinition enclosingType = fieldElement.getEnclosingType();
        ClassContext classContext = enclosingType.getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        ValueType fieldType = fieldElement.getType();
        String itemName = "static-" + enclosingType.getInternalName().replace('/', '.') + "-" + fieldElement.getName() + "-" + fieldElement.getIndex();
        symbol = ctxt.getLiteralFactory().literalOfSymbol(itemName, fieldType.getPointer());
        SymbolLiteral appearing = staticFields.putIfAbsent(fieldElement, symbol);
        if (appearing != null) {
            return appearing;
        }
        Section section = ctxt.getOrAddProgramModule(enclosingType).getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
        Literal initialValue = fieldElement.getInitialValue();
        Linkage linkage = Linkage.INTERNAL;
        if (initialValue == null || initialValue instanceof ZeroInitializerLiteral) {
            linkage = Linkage.COMMON;
            initialValue = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(fieldType);
        }
        section.addData(fieldElement, itemName, initialValue).setLinkage(linkage);
        return symbol;
    }
}
