package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ValueHandle;
import org.qbicc.object.Section;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;

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
    public ValueHandle staticField(FieldElement field) {
        GlobalVariableElement global = Lowering.get(ctxt).getGlobalForField(field);
        DefinedTypeDefinition fieldHolder = field.getEnclosingType();
        if (! fieldHolder.equals(ourHolder)) {
            // we have to declare it in our translation unit
            Section section = ctxt.getOrAddProgramModule(ourHolder).getOrAddSection(global.getSection());
            section.declareData(field, global.getName(), global.getType());
        }
        return globalVariable(global);
    }
}
