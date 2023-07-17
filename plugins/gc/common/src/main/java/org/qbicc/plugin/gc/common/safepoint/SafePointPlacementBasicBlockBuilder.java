package org.qbicc.plugin.gc.common.safepoint;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * Block builder which places safepoint polls.
 */
public final class SafePointPlacementBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private SafePointPlacementBasicBlockBuilder(BasicBlockBuilder delegate) {
        super(delegate);
    }

    public static BasicBlockBuilder createIfNeeded(FactoryContext ctxt, BasicBlockBuilder delegate) {
        final ExecutableElement currentElement = delegate.element();
        final boolean noSafePoints = currentElement.hasAllModifiersOf(ClassFile.I_ACC_NO_SAFEPOINTS);
        // functions should not automatically poll for safepoints, because there might not be a thread
        return currentElement instanceof FunctionElement || noSafePoints ? delegate : new SafePointPlacementBasicBlockBuilder(delegate);
    }

    @Override
    public BasicBlock return_(Value value) {
        safePoint();
        return super.return_(value);
    }

    @Override
    public BasicBlock throw_(Value value) {
        safePoint();
        return super.throw_(value);
    }
}
