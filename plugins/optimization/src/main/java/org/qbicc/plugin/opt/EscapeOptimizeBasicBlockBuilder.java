package org.qbicc.plugin.opt;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

public class EscapeOptimizeBasicBlockBuilder implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
    private final CompilationContext ctxt;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;

    public EscapeOptimizeBasicBlockBuilder(final CompilationContext ctxt, final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate) {
        this.ctxt = ctxt;
        this.delegate = delegate;
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public Value visit(Node.Copier param, New original) {
        // TODO temporarily limit to example for easier testing
        if (EscapeAnalysis.get(ctxt).notEscapingMethod(original) && original.getClassObjectType().toString().contains("example")) {
            return stackAllocate(original.getClassObjectType(), param.getBlockBuilder());
        }

        return NodeVisitor.Delegating.super.visit(param, original);
    }

    private Value stackAllocate(ClassObjectType type, BasicBlockBuilder bbb) {
        System.out.println("New on type " + type + " does not escape thread, stack allocate");
        // TODO instruct stack allocation if not escaping thread
        // TODO: Customise this generated block

        // Copied from NoGcBasicBlockBuilder
        Layout layout = Layout.get(ctxt);
        Layout.LayoutInfo info = layout.getInstanceLayoutInfo(type.getDefinition());
        CompoundType compoundType = info.getCompoundType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerLiteral align = lf.literalOf(compoundType.getAlign());

        // TODO David Lloyd: the alignment should be the minimum alignment of the lowered target type
        // TODO David Lloyd: which we don't know until lower, but perhaps ObjectAccessLoweringBuilder could intercept that and set the minimum alignment to the minimum of the layout
        // TODO Galder: Right now I'm working on ADD/ANALYSIS, I could move to ANALYSIS/LOWER as you suggested in a call
        Value ptrVal = bbb.stackAllocate(compoundType, lf.literalOf(1), align);

        Value oop = bbb.valueConvert(ptrVal, type.getReference());
        ValueHandle oopHandle = bbb.referenceHandle(oop);

        // zero initialize the object's instance fields (but not the header fields that are defined in java.lang.Object)
        LoadedTypeDefinition curClass = type.getDefinition().load();
        while (curClass.hasSuperClass()) {
            curClass.eachField(f -> {
                if (!f.isStatic()) {
                    bbb.store(bbb.instanceFieldOf(oopHandle, f), lf.zeroInitializerLiteralOfType(f.getType()), MemoryAtomicityMode.UNORDERED);
                }
            });
            curClass = curClass.getSuperClass();
        }

        // now initialize the object header (aka fields of java.lang.Object)
        initializeObjectHeader(oopHandle, layout, type.getDefinition().load().getType(), bbb);

        return oop;
    }

    private void initializeObjectHeader(ValueHandle oopHandle, Layout layout, ObjectType objType, BasicBlockBuilder bbb) {
        FieldElement typeId = layout.getObjectTypeIdField();
        bbb.store(bbb.instanceFieldOf(oopHandle, typeId),  ctxt.getLiteralFactory().literalOfType(objType), MemoryAtomicityMode.UNORDERED);
    }

}