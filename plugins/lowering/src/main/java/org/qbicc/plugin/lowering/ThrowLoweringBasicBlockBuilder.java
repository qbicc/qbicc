package org.qbicc.plugin.lowering;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.type.FunctionType;
import org.qbicc.type.MethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.element.FieldElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        TypeSystem ts = ctxt.getTypeSystem();
        ThrowExceptionHelper teh = ThrowExceptionHelper.get(ctxt);
        FieldElement exceptionField = ctxt.getExceptionField();
        store(instanceFieldOf(referenceHandle(load(currentThread(), SingleUnshared)), exceptionField), value, SingleUnshared);

        // TODO Is this safe? Can the java/lang/Thread object be moved while this pointer is still in use?
        Value ptr = bitCast(addressOf(instanceFieldOf(referenceHandle(load(currentThread(), SingleUnshared)), teh.getUnwindExceptionField())), teh.getUnwindExceptionField().getType().getPointer());

        String functionName = "_Unwind_RaiseException";
        MethodType origType = teh.getRaiseExceptionMethod().getType();
        FunctionType functionType = ts.getFunctionType(origType.getReturnType(), origType.getParameterTypes());
        FunctionDeclaration decl = ctxt.getImplicitSection(getRootElement()).declareFunction(teh.getRaiseExceptionMethod(), functionName, functionType);
        return callNoReturn(pointerHandle(ctxt.getLiteralFactory().literalOf(decl)), List.of(ptr));
    }
}
