package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.object.Section;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

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
            ValueType fieldType = fieldElement.getType();
            Section section = ctxt.getImplicitSection(ourHolder);
            section.declareData(fieldElement, symbol.getName(), fieldType);
        }
        // todo: replace with global variable
        return pointerHandle(symbol);
    }
}
