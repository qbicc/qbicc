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
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.pointer.ElementPointer;
import org.qbicc.pointer.ExecutableElementPointer;
import org.qbicc.pointer.GlobalPointer;
import org.qbicc.pointer.InstanceFieldPointer;
import org.qbicc.pointer.MemberPointer;
import org.qbicc.pointer.MemoryPointer;
import org.qbicc.pointer.OffsetPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.StaticFieldElement;

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
        if (pointer instanceof ExecutableElementPointer eep) {
            ExecutableElement ee = eep.getExecutableElement();
            Function function = ctxt.getExactFunction(ee);
            ProgramModule programModule = ctxt.getOrAddProgramModule(copier.getBlockBuilder().getCurrentElement().getEnclosingType());
            FunctionDeclaration decl = programModule.declareFunction(function);
            return ProgramObjectPointer.of(decl);
        } else if (pointer instanceof StaticFieldPointer sfp) {
            StaticFieldElement field = sfp.getStaticField();
            GlobalVariableElement global = Lowering.get(ctxt).getGlobalForStaticField(field);
            ProgramModule programModule = ctxt.getOrAddProgramModule(copier.getBlockBuilder().getCurrentElement().getEnclosingType());
            DataDeclaration decl = programModule.declareData(field, global.getName(), global.getType());
            return ProgramObjectPointer.of(decl);
        } else if (pointer instanceof GlobalPointer gp) {
            GlobalVariableElement global = gp.getGlobalVariable();
            ProgramModule programModule = ctxt.getOrAddProgramModule(copier.getBlockBuilder().getCurrentElement().getEnclosingType());
            DataDeclaration decl = programModule.declareData(null, global.getName(), global.getType());
            return ProgramObjectPointer.of(decl);
        } else if (pointer instanceof ElementPointer ep) {
            return new ElementPointer(lowerPointer(copier, ep.getArrayPointer()), ep.getIndex());
        } else if (pointer instanceof MemberPointer mp) {
            return new MemberPointer(lowerPointer(copier, mp.getStructurePointer()), mp.getMember());
        } else if (pointer instanceof OffsetPointer op) {
            return lowerPointer(copier, op.getBasePointer()).offsetByElements(op.getOffset());
        } else if (pointer instanceof InstanceFieldPointer ifp) {
            InstanceFieldElement field = ifp.getInstanceField();
            return new MemberPointer(lowerPointer(copier, ifp.getObjectPointer()), Layout.get(ctxt).getInstanceLayoutInfo(field.getEnclosingType()).getMember(field));
        } else if (pointer instanceof MemoryPointer) {
            throw new UnsupportedOperationException("Lowering arbitrary memory is not supported");
        } else {
            return pointer;
        }
    }

}
