package org.qbicc.plugin.lowering;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Value;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.element.FieldElement;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        ThrowExceptionHelper teh = ThrowExceptionHelper.get(ctxt);
        FieldElement exceptionField = ctxt.getExceptionField();
        store(instanceFieldOf(referenceHandle(currentThread()), exceptionField), value, MemoryAtomicityMode.NONE);

        // TODO Is this safe? Can the java/lang/Thread object be moved while this pointer is still in use?
        Value ptr = bitCast(addressOf(instanceFieldOf(referenceHandle(currentThread()), teh.getUnwindExceptionField())), teh.getUnwindExceptionField().getType().getPointer());

        String functionName = "_Unwind_RaiseException";
        FunctionType functionType = teh.getRaiseExceptionMethod().getType();
        FunctionDeclaration decl = ctxt.getImplicitSection(getRootElement()).declareFunction(teh.getRaiseExceptionMethod(), functionName, functionType);
        return callNoReturn(pointerHandle(ctxt.getLiteralFactory().literalOf(decl)), List.of(ptr));
    }
}
