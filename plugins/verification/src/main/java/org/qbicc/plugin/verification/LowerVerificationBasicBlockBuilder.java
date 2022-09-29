package org.qbicc.plugin.verification;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.element.InitializerElement;

/**
 * A block builder that forbids lowering of high-level (first phase) nodes in order to keep the back end(s) as simple
 * as possible.
 */
public class LowerVerificationBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public LowerVerificationBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        invalidNode("throw");
        return return_();
    }

    public BasicBlock jsr(final BlockLabel subLabel, final BlockLiteral returnAddress, Map<Slot, Value> targetArguments) {
        invalidNode("jsr");
        return goto_(returnAddress.getBlockLabel(), Map.of());
    }

    public BasicBlock ret(final Value address, Map<Slot, Value> targetArguments) {
        invalidNode("ret");
        return return_();
    }

    public Node monitorEnter(final Value obj) {
        invalidNode("monitorEnter");
        return nop();
    }

    public Node monitorExit(final Value obj) {
        invalidNode("monitorExit");
        return nop();
    }

    public Node initCheck(InitializerElement initializer, Value initThunk) {
        invalidNode("runtimeInitCheck");
        return nop();
    }

    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        invalidNode("new");
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(type.getReference());
    }

    public Value newArray(final PrimitiveArrayObjectType arrayType, final Value size) {
        invalidNode("newArray");
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(arrayType.getReference());
    }

    public Value newReferenceArray(final ReferenceArrayObjectType arrayType, final Value elemTypeId, final Value dimensions, final Value size) {
        invalidNode("newReferenceArray");
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(arrayType.getReference());
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        invalidNode("multiNewArray");
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(arrayType.getReference());
    }

    private void invalidNode(String name) {
        ctxt.warning(getLocation(), "Invalid node encountered (cannot directly lower %s)", name);
    }
}
