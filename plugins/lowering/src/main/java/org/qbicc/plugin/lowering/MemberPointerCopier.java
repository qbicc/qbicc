package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.GlobalVariableLiteral;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.StaticFieldElement;

/**
 *
 */
public final class MemberPointerCopier implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock> {
    private final CompilationContext ctxt;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate;

    public MemberPointerCopier(CompilationContext ctxt, NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate) {
        this.ctxt = ctxt;
        this.delegate = delegate;
    }

    @Override
    public NodeVisitor<Node.Copier, Value, Node, BasicBlock> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public Value visit(Node.Copier copier, GlobalVariableLiteral literal) {
        GlobalVariableElement global = literal.getVariableElement();
        ProgramModule programModule = ctxt.getOrAddProgramModule(copier.getBlockBuilder().getCurrentElement().getEnclosingType());
        DataDeclaration decl = programModule.declareData(null, global.getName(), global.getType());
        return ctxt.getLiteralFactory().literalOf(decl);
    }

    @Override
    public Value visit(Node.Copier copier, StaticFieldLiteral node) {
        StaticFieldElement field = node.getVariableElement();
        GlobalVariableElement global = BuildtimeHeap.get(ctxt).getGlobalForStaticField(field);
        DefinedTypeDefinition fieldHolder = field.getEnclosingType();
        DefinedTypeDefinition ourHolder = copier.getBlockBuilder().getRootElement().getEnclosingType();
        if (! fieldHolder.equals(ourHolder)) {
            // we have to declare it in our translation unit
            ProgramModule programModule = ctxt.getOrAddProgramModule(ourHolder);
            programModule.declareData(field, global.getName(), global.getType());
        }
        return ctxt.getLiteralFactory().literalOf(global);
    }
}
