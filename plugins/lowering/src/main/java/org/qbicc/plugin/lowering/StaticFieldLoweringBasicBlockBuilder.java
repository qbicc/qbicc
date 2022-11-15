package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ValueHandle;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.StaticFieldElement;

/**
 *
 */
public class StaticFieldLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition ourHolder;

    public StaticFieldLoweringBasicBlockBuilder(final FactoryContext fc, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
        ourHolder = getCurrentElement().getEnclosingType();
    }

    @Override
    public ValueHandle staticField(FieldElement field) {
        if (! field.isStatic()) {
            throw new IllegalArgumentException();
        }
        GlobalVariableElement global = BuildtimeHeap.get(ctxt).getGlobalForStaticField((StaticFieldElement) field);
        DefinedTypeDefinition fieldHolder = field.getEnclosingType();
        if (! fieldHolder.equals(ourHolder)) {
            // we have to declare it in our translation unit
            ProgramModule programModule = ctxt.getOrAddProgramModule(ourHolder);
            programModule.declareData(field, global.getName(), global.getType());
        }
        return pointerHandle(getLiteralFactory().literalOf(global));
    }
}
