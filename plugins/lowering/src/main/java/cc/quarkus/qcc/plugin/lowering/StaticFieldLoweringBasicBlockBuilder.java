package cc.quarkus.qcc.plugin.lowering;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.MemoryAccessMode;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public class StaticFieldLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public StaticFieldLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value readStaticField(final FieldElement fieldElement, final JavaAccessMode mode) {
        MemoryAtomicityMode atomicityMode = mode == JavaAccessMode.VOLATILE ? MemoryAtomicityMode.ACQUIRE : MemoryAtomicityMode.UNORDERED;
        return pointerLoad(getFieldPointer(fieldElement), MemoryAccessMode.PLAIN, atomicityMode);
    }

    public Node writeStaticField(final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        MemoryAtomicityMode atomicityMode = mode == JavaAccessMode.VOLATILE ? MemoryAtomicityMode.ACQUIRE : MemoryAtomicityMode.UNORDERED;
        return pointerStore(getFieldPointer(fieldElement), value, MemoryAccessMode.PLAIN, atomicityMode);
    }

    private Value getFieldPointer(final FieldElement fieldElement) {
        SymbolLiteral symbol = LoweredStaticFields.get(ctxt).getSymbolForField(fieldElement);
        DefinedTypeDefinition fieldHolder = fieldElement.getEnclosingType();
        DefinedTypeDefinition ourHolder = getCurrentElement().getEnclosingType();
        if (! fieldHolder.equals(ourHolder)) {
            // we have to declare it in our translation unit
            ValueType fieldType = fieldElement.getType(fieldHolder.getContext(), List.of());
            Section section = ctxt.getOrAddProgramModule(ourHolder).getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
            section.declareData(fieldElement, symbol.getName(), fieldType);
        }
        return symbol;
    }
}
