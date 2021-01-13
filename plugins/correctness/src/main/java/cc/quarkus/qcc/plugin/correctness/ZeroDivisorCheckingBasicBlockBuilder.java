package cc.quarkus.qcc.plugin.correctness;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

import java.util.List;

/**
 * This builder checks for zero divisor in integer divisions.
 */
public class ZeroDivisorCheckingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ZeroDivisorCheckingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value divide(Value v1, Value v2) {
        ValueType v1Type = v1.getType();
        ValueType v2Type = v2.getType();
        if (v1Type instanceof IntegerType && v2Type instanceof IntegerType) {
            LiteralFactory lf = ctxt.getLiteralFactory();
            final IntegerLiteral zero = lf.literalOf(0);
            final BlockLabel throwIt = new BlockLabel();
            final BlockLabel goAhead = new BlockLabel();

            if_(cmpEq(v2, zero), throwIt, goAhead);
            begin(throwIt);
            ClassContext classContext = getCurrentElement().getEnclosingType().getContext();
            ValidatedTypeDefinition ae = classContext.findDefinedType("java/lang/ArithmeticException").validate();
            Value ex = new_(ae.getClassType());
            ex = invokeConstructor(ex, ae.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
            throw_(ex); // Throw java.lang.ArithmeticException
            begin(goAhead);
        }
        return super.divide(v1, v2);
    }
}
