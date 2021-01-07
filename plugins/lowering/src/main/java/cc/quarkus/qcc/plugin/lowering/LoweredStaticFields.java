package cc.quarkus.qcc.plugin.lowering;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.FieldElement;

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
        ValueType fieldType = fieldElement.getType(List.of());
        String itemName = "static-" + enclosingType.getInternalName().replace('/', '.') + "-" + fieldElement.getName() + "-" + fieldElement.getIndex();
        symbol = ctxt.getLiteralFactory().literalOfSymbol(itemName, fieldType.getPointer());
        SymbolLiteral appearing = staticFields.putIfAbsent(fieldElement, symbol);
        if (appearing != null) {
            return appearing;
        }
        Section section = ctxt.getOrAddProgramModule(enclosingType).getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
        Literal initialValue = fieldElement.getInitialValue();
        if (initialValue == null) {
            initialValue = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(fieldType);
        }
        section.addData(fieldElement, itemName, initialValue);
        return symbol;
    }
}
