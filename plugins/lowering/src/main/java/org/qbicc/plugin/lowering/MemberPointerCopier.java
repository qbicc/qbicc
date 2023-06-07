package org.qbicc.plugin.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.EncodeReferenceLiteral;
import org.qbicc.graph.literal.StructLiteral;
import org.qbicc.graph.literal.ElementOfLiteral;
import org.qbicc.graph.literal.GlobalVariableLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.MemberOfLiteral;
import org.qbicc.graph.literal.OffsetFromLiteral;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.StructType;
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
    public Value visit(Node.Copier copier, BitCastLiteral literal) {
        return ctxt.getLiteralFactory().bitcastLiteral((Literal) copier.copyValue(literal.getValue()), literal.getType());
    }

    @Override
    public Value visit(Node.Copier copier, ElementOfLiteral literal) {
        return ctxt.getLiteralFactory().elementOfLiteral(
            (Literal) copier.copyValue(literal.getArrayPointer()),
            (Literal) copier.copyValue(literal.getIndex())
        );
    }

    @Override
    public Value visit(Node.Copier copier, MemberOfLiteral literal) {
        return ctxt.getLiteralFactory().memberOfLiteral((Literal) copier.copyValue(literal.getStructurePointer()), literal.getMember());
    }

    @Override
    public Value visit(Node.Copier copier, OffsetFromLiteral literal) {
        return ctxt.getLiteralFactory().offsetFromLiteral(
            (Literal) copier.copyValue(literal.getBasePointer()),
            (Literal) copier.copyValue(literal.getOffset())
        );
    }

    @Override
    public Value visit(Node.Copier copier, EncodeReferenceLiteral literal) {
        return ctxt.getLiteralFactory().encodeReferenceLiteral((Literal) copier.copyValue(literal.getValue()), literal.getType());
    }

    @Override
    public Literal visit(Node.Copier copier, GlobalVariableLiteral literal) {
        GlobalVariableElement global = literal.getVariableElement();
        ProgramModule programModule = ctxt.getOrAddProgramModule(copier.getBlockBuilder().getCurrentElement().getEnclosingType());
        DataDeclaration decl = programModule.declareData(null, global.getName(), global.getType());
        return ctxt.getLiteralFactory().literalOf(decl);
    }

    @Override
    public Literal visit(Node.Copier copier, StaticFieldLiteral node) {
        StaticFieldElement field = node.getVariableElement();
        GlobalVariableElement global = BuildtimeHeap.get(ctxt).getGlobalForStaticField(field);
        ProgramModule programModule = ctxt.getOrAddProgramModule(copier.getBlockBuilder().getRootElement().getEnclosingType());
        final DataDeclaration decl = programModule.declareData(field, global.getName(), global.getType());
        return ctxt.getLiteralFactory().literalOf(decl);
    }

    @Override
    public Value visit(Node.Copier copier, ArrayLiteral literal) {
        // an array literal may be composed of literals that must be transformed
        final List<Literal> values = literal.getValues();
        ArrayList<Literal> newList = new ArrayList<>(values.size());
        for (Literal value : values) {
            newList.add((Literal) copier.copyValue(value));
        }
        return ctxt.getLiteralFactory().literalOf(literal.getType(), newList);
    }

    @Override
    public Value visit(Node.Copier copier, StructLiteral literal) {
        // a compound literal may be composed of literals that must be transformed
        final Map<StructType.Member, Literal> originalValues = literal.getValues();
        final HashMap<StructType.Member, Literal> newMap = new HashMap<>(originalValues.size());
        for (Map.Entry<StructType.Member, Literal> entry : originalValues.entrySet()) {
            newMap.put(entry.getKey(), (Literal) copier.copyValue(entry.getValue()));
        }
        return ctxt.getLiteralFactory().literalOf(literal.getType(), newMap);
    }
}
