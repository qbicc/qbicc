package org.qbicc.plugin.opt.ea;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

public final class EscapeAnalysisOptimizeVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
    private final CompilationContext ctxt;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;
    private final MethodElement zeroMethod;

    public EscapeAnalysisOptimizeVisitor(final CompilationContext ctxt, final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate) {
        this.ctxt = ctxt;
        this.delegate = delegate;

        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition defined = classContext.findDefinedType("org/qbicc/runtime/gc/nogc/NoGcHelpers");
        if (defined == null) {
            throw runtimeMissing();
        }
        LoadedTypeDefinition loaded = defined.load();
        int index = loaded.findMethodIndex(e -> e.getName().equals("clear"));
        if (index == -1) {
            throw methodMissing();
        }
        zeroMethod = loaded.getMethod(index);
    }

    private static IllegalStateException runtimeMissing() {
        return new IllegalStateException("The NoGC helpers runtime classes are not present in the bootstrap class path");
    }

    private static IllegalStateException methodMissing() {
        return new IllegalStateException("Required method is missing from the NoGC helpers");
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public Value visit(Node.Copier param, New original) {
        final BasicBlockBuilder bbb = param.getBlockBuilder();
        if (isStackAllocate(original, bbb)) {
            // Copy dependency so that stack allocation can be scheduled in the right place
            param.copyNode(original.getDependency());
            return stackAllocate(original, bbb);
        }

        return NodeVisitor.Delegating.super.visit(param, original);
    }

    private boolean isStackAllocate(New new_, BasicBlockBuilder bbb) {
        return EscapeAnalysisState.get(ctxt).isNotEscapingMethod(new_, bbb.getCurrentElement())
            && notInLoop(new_);
    }

    private boolean notInLoop(Node node) {
        if (node instanceof OrderedNode on) {
            final Node dependency = on.getDependency();
            if (dependency instanceof BlockEntry be) {
                return BlockLabel.getTargetOf(be.getPinnedBlockLabel()).getLoops().size() == 0;
            }
            return notInLoop(on.getDependency());
        }

        return false;
    }

    private Value stackAllocate(New new_, BasicBlockBuilder bbb) {
        ClassObjectType type = new_.getClassObjectType();

        // Copied and adjusted from NoGcBasicBlockBuilder
        Layout layout = Layout.get(ctxt);
        LayoutInfo info = layout.getInstanceLayoutInfo(type.getDefinition());
        CompoundType compoundType = info.getCompoundType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerLiteral align = lf.literalOf(compoundType.getAlign());

        // TODO David Lloyd: the alignment should be the minimum alignment of the lowered target type
        // TODO David Lloyd: which we don't know until lower, but perhaps ObjectAccessLoweringBuilder could intercept that and set the minimum alignment to the minimum of the layout
        Value ptrVal = bbb.stackAllocate(compoundType, lf.literalOf(1), align);

        Value oop = bbb.valueConvert(ptrVal, type.getReference());

        // zero initialize the object's instance fields
        initializeObjectFieldsToZero(info, lf, oop, bbb);
        // initialize object header
        initializeObjectHeader(CoreClasses.get(ctxt), bbb.referenceHandle(oop), ctxt.getLiteralFactory().literalOfType(type), bbb);

        return oop;
    }

    private void initializeObjectFieldsToZero(final LayoutInfo info, final LiteralFactory lf, final Value oop, final BasicBlockBuilder bbb) {
        bbb.call(bbb.staticMethod(zeroMethod, zeroMethod.getDescriptor(), zeroMethod.getType()), List.of(oop, lf.literalOf(info.getCompoundType().getSize())));
    }

    // TODO copied from BasicInitializationBasicBlockBuilder
    private void initializeObjectHeader(final CoreClasses coreClasses, final ValueHandle handle, final Value typeId, final BasicBlockBuilder bbb) {
        bbb.store(bbb.instanceFieldOf(handle, coreClasses.getObjectTypeIdField()), typeId, MemoryAtomicityMode.UNORDERED);
        FieldElement monitorField = coreClasses.getObjectNativeObjectMonitorField();
        bbb.store(bbb.instanceFieldOf(handle, monitorField), ctxt.getLiteralFactory().literalOf((IntegerType)monitorField.getType(), 0L), MemoryAtomicityMode.NONE);
    }

}
