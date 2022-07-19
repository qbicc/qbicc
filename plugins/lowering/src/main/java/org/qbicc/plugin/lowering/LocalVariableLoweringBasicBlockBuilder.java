package org.qbicc.plugin.lowering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.BooleanType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.LocalVariableElement;

/**
 *
 */
public final class LocalVariableLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final Collection<LocalVariableElement> usedVariables;
    private final Map<LocalVariableElement, Value> allocatedVariables = new HashMap<>();
    private boolean started;

    public LocalVariableLoweringBasicBlockBuilder(FactoryContext fc, BasicBlockBuilder delegate) {
        super(delegate);
        CompilationContext ctxt = getContext();
        this.ctxt = ctxt;
        this.usedVariables = Lowering.get(ctxt).removeUsedVariableSet(getCurrentElement());
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        final Node node = super.begin(blockLabel);
        if (! started) {
            setUp();
        }
        return node;
    }

    @Override
    public <T> BasicBlock begin(BlockLabel blockLabel, T arg, BiConsumer<T, BasicBlockBuilder> maker) {
        if (! started) {
            return super.begin(blockLabel, bbb -> {
                setUp();
                maker.accept(arg, bbb);
            });
        } else {
            return super.begin(blockLabel, arg, maker);
        }
    }

    private void setUp() {
        started = true;
        final UnsignedIntegerType unsignedInteger8Type = ctxt.getTypeSystem().getUnsignedInteger8Type();
        final LiteralFactory lf = ctxt.getLiteralFactory();
        final IntegerLiteral one = lf.literalOf(1);
        // todo: allocate local variables closer to where they are used
        for (LocalVariableElement lve : usedVariables) {
            ValueType varType = lve.getType();
            if (varType instanceof BooleanType) {
                // widen booleans to 8 bits
                varType = unsignedInteger8Type;
            }
            int oldLine = setLineNumber(lve.getLine());
            int oldBci = setBytecodeIndex(lve.getBci());
            try {
                Value address = stackAllocate(varType, one, lf.literalOf(varType.getAlign()));
                allocatedVariables.put(lve, address);
                declareDebugAddress(lve, address);
            } finally {
                setLineNumber(oldLine);
                setBytecodeIndex(oldBci);
            }
        }
    }

    @Override
    public PointerValue localVariable(LocalVariableElement variable) {
        final Value pointer = allocatedVariables.get(variable);
        if (pointer == null) {
            throw new NoSuchElementException();
        }
        return pointerValueOf(pointer);
    }
}
