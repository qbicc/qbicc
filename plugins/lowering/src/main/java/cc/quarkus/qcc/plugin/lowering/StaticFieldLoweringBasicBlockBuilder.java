package cc.quarkus.qcc.plugin.lowering;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.generic.TypeParameterContext;

/**
 *
 */
public class StaticFieldLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition ourHolder;

    public StaticFieldLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        ourHolder = getCurrentElement().getEnclosingType();
    }

    @Override
    public ValueHandle staticField(FieldElement fieldElement) {
        SymbolLiteral symbol = LoweredStaticFields.get(ctxt).getSymbolForField(fieldElement);
        DefinedTypeDefinition fieldHolder = fieldElement.getEnclosingType();
        if (! fieldHolder.equals(ourHolder)) {
            // we have to declare it in our translation unit
            ValueType fieldType = fieldElement.getType(TypeParameterContext.EMPTY);
            Section section = ctxt.getImplicitSection(ourHolder);
            section.declareData(fieldElement, symbol.getName(), fieldType);
        }
        // todo: replace with global variable
        return pointerHandle(symbol);
    }
}
