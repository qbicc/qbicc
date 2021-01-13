package cc.quarkus.qcc.plugin.correctness;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

import java.util.List;

/**
 * This builder checks array index out of bounds exceptions.
 */
public class ArrayIndexOutOfBoundsCheckingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ArrayIndexOutOfBoundsCheckingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value readArrayValue(Value array, Value index, JavaAccessMode mode) {
        indexOutOfBoundsCheck(array, index);
        return super.readArrayValue(array, index, mode);
    }

    @Override
    public Node writeArrayValue(Value array, Value index, Value value, JavaAccessMode mode) {
        indexOutOfBoundsCheck(array, index);
        return super.writeArrayValue(array, index, value, mode);
    }

    private void indexOutOfBoundsCheck(Value array, Value index) {
        ValueType arrayType = array.getType();
        if (arrayType instanceof ArrayType) {
            return;
        }

        final BlockLabel notNegative = new BlockLabel();
        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();

        LiteralFactory lf = ctxt.getLiteralFactory();
        final IntegerLiteral zero = lf.literalOf(0);
        if_(cmpLt(index, zero), throwIt, notNegative);
        begin(notNegative);
        final Value length = arrayLength(array);
        if_(cmpGe(index, length), throwIt, goAhead);
        begin(throwIt);
        ClassContext classContext = getCurrentElement().getEnclosingType().getContext();
        ValidatedTypeDefinition aiobe = classContext.findDefinedType("java/lang/ArrayIndexOutOfBoundsException").validate();
        Value ex = new_(aiobe.getClassType());
        ex = invokeConstructor(ex, aiobe.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
        throw_(ex); // Throw java.lang.ArrayIndexOutOfBoundsException
        begin(goAhead);
    }

}
