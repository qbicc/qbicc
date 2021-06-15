package org.qbicc.plugin.lowering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.definition.element.LocalVariableElement;

/**
 *
 */
public final class LocalVariableLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final Collection<LocalVariableElement> usedVariables;
    private final Map<LocalVariableElement, Value> allocatedVariables = new HashMap<>();
    private boolean started;

    public LocalVariableLoweringBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        this.usedVariables = Lowering.get(ctxt).removeUsedVariableSet(getCurrentElement());
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        final Node node = super.begin(blockLabel);
        if (! started) {
            started = true;
            final LiteralFactory lf = ctxt.getLiteralFactory();
            final IntegerLiteral one = lf.literalOf(1);
            // todo: allocate local variables closer to where they are used
            for (LocalVariableElement lve : usedVariables) {
                allocatedVariables.put(lve, stackAllocate(lve.getType(), one, lf.literalOf(lve.getType().getAlign())));
            }
        }
        return node;
    }

    @Override
    public ValueHandle localVariable(LocalVariableElement variable) {
        final Value pointer = allocatedVariables.get(variable);
        if (pointer == null) {
            throw new NoSuchElementException();
        }
        return super.pointerHandle(pointer);
    }
}
