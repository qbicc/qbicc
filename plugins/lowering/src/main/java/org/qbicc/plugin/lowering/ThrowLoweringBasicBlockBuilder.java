package org.qbicc.plugin.lowering;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.element.FieldElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        BasicBlockBuilder fb = getFirstBuilder();
        ThrowExceptionHelper teh = ThrowExceptionHelper.get(ctxt);
        FieldElement exceptionField = ctxt.getExceptionField();
        fb.store(fb.instanceFieldOf(fb.referenceHandle(fb.load(fb.currentThread(), SingleUnshared)), exceptionField), value, SingleUnshared);

        // TODO Is this safe? Can the java/lang/Thread object be moved while this pointer is still in use?
        Value ptr = fb.bitCast(fb.addressOf(fb.instanceFieldOf(fb.referenceHandle(fb.load(fb.currentThread(), SingleUnshared)), teh.getUnwindExceptionField())), teh.getUnwindExceptionField().getType().getPointer());
        return fb.callNoReturn(fb.staticMethod(teh.getRaiseExceptionMethod()), List.of(ptr));
    }
}
