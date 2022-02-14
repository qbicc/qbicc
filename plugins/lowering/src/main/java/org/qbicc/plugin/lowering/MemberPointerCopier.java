package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Function;
import org.qbicc.object.Section;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.pointer.ElementPointer;
import org.qbicc.pointer.InstanceFieldPointer;
import org.qbicc.pointer.MemberPointer;
import org.qbicc.pointer.MemoryPointer;
import org.qbicc.pointer.OffsetPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public final class MemberPointerCopier implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
    private final CompilationContext ctxt;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;

    public MemberPointerCopier(CompilationContext ctxt, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate) {
        this.ctxt = ctxt;
        this.delegate = delegate;
    }

    @Override
    public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public Value visit(Node.Copier param, PointerLiteral node) {
        Pointer oldPtr = node.getPointer();
        Pointer newPtr = lowerPointer(param, oldPtr);
        return newPtr == oldPtr ? getDelegateValueVisitor().visit(param, node) : ctxt.getLiteralFactory().literalOf(newPtr);
    }

    private Pointer lowerPointer(Node.Copier copier, Pointer pointer) {
        if (pointer instanceof StaticMethodPointer smp) {
            MethodElement method = smp.getStaticMethod();
            Function function = ctxt.getExactFunction(method);
            Section section = ctxt.getImplicitSection(copier.getBlockBuilder().getCurrentElement());
            DataDeclaration decl = section.declareData(function);
            return ProgramObjectPointer.of(decl);
        } else if (pointer instanceof StaticFieldPointer sfp) {
            FieldElement field = sfp.getStaticField();
            GlobalVariableElement global = Lowering.get(ctxt).getStaticsGlobalForType(field.getEnclosingType().load());
            DefinedTypeDefinition fieldHolder = field.getEnclosingType();
            Section section = ctxt.getImplicitSection(copier.getBlockBuilder().getCurrentElement());
            DataDeclaration decl = section.declareData(field, global.getName(), global.getType());
            LayoutInfo layoutInfo = Layout.get(ctxt).getStaticLayoutInfo(fieldHolder);
            assert layoutInfo != null; // field exists so layout is non empty
            return new MemberPointer(ProgramObjectPointer.of(decl), layoutInfo.getMember(field));
        } else if (pointer instanceof ElementPointer ep) {
            return new ElementPointer(lowerPointer(copier, ep.getArrayPointer()), ep.getIndex());
        } else if (pointer instanceof MemberPointer mp) {
            return new MemberPointer(lowerPointer(copier, mp.getStructurePointer()), mp.getMember());
        } else if (pointer instanceof OffsetPointer op) {
            return lowerPointer(copier, op.getBasePointer()).offsetByElements(op.getOffset());
        } else if (pointer instanceof InstanceFieldPointer ifp) {
            FieldElement field = ifp.getFieldElement();
            return new MemberPointer(lowerPointer(copier, ifp.getObjectPointer()), Layout.get(ctxt).getInstanceLayoutInfo(field.getEnclosingType()).getMember(field));
        } else if (pointer instanceof MemoryPointer) {
            throw new UnsupportedOperationException("Lowering arbitrary memory is not supported");
        } else {
            return pointer;
        }
    }

}
