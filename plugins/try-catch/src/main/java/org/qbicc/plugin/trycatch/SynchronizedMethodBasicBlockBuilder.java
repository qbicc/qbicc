package org.qbicc.plugin.trycatch;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A basic block builder which adds a monitor acquire to the start of the subprogram and adds a monitor release
 * to all possible exit paths.
 */
public class SynchronizedMethodBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final Value monitor;
    private boolean started;
    private final ReferenceType throwable;

    private SynchronizedMethodBasicBlockBuilder(final BasicBlockBuilder delegate) {
        super(delegate);
        ExecutableElement element = getCurrentElement();
        DefinedTypeDefinition enclosing = element.getEnclosingType();
        if (element.isStatic()) {
            monitor = classOf(getLiteralFactory().literalOfType(enclosing.load().getObjectType()), getLiteralFactory().zeroInitializerLiteralOfType(getTypeSystem().getUnsignedInteger8Type()));
        } else {
            monitor = notNull(parameter(enclosing.load().getObjectType().getReference(), "this", 0));
        }
        throwable = getContext().getBootstrapClassContext().findDefinedType("java/lang/Throwable").load().getClassType().getReference();
    }

    public Node begin(final BlockLabel blockLabel) {
        Node node = super.begin(blockLabel);
        if (! started) {
            started = true;
            Node monitorEnter = monitorEnter(monitor);
            return monitorEnter;
        }
        return node;
    }

    @Override
    public <T> BasicBlock begin(BlockLabel blockLabel, T arg, BiConsumer<T, BasicBlockBuilder> maker) {
        if (! started) {
            // method start
            return super.begin(blockLabel, bbb -> {
                started = true;
                monitorEnter(monitor);
            });
        } else {
            return super.begin(blockLabel, arg, maker);
        }
    }

    public BasicBlock return_(final Value value) {
        monitorExit(monitor);
        return super.return_(value);
    }

    public BasicBlock throw_(final Value value) {
        // note: this must come *after* the local throw handling builder otherwise problems will result;
        // local throw will transform `throw` to `goto`, resulting in extra monitor releases
        monitorExit(monitor);
        return super.throw_(value);
    }

    public static BasicBlockBuilder createIfNeeded(FactoryContext fc, BasicBlockBuilder delegate) {
        if (delegate.getCurrentElement().hasAllModifiersOf(ClassFile.ACC_SYNCHRONIZED)) {
            return new SynchronizedMethodBasicBlockBuilder(delegate);
        } else {
            return delegate;
        }
    }
}
