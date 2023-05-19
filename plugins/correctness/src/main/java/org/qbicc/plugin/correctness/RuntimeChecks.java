package org.qbicc.plugin.correctness;

import java.util.List;
import java.util.Map;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.StructType;
import org.qbicc.type.NullableType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * A handy utility class to do some common runtime checks.
 */
public final class RuntimeChecks {
    public static final String NPE = "java/lang/NullPointerException";

    private static final Slot TEMP0 = Slot.temp(0);

    private RuntimeChecks() {
    }

    /**
     * Returns a {@code Value} that is {@code true} if the given {@code value} is {@code null}, or {@code false} if
     * it is not {@code null}.
     * If the value is known to be non-{@code null}, then the literal {@code false} is returned.
     *
     * @param bbb the basic block builder to use (must not be {@code null})
     * @param value the value to evaluate (must not be {@code null})
     * @return the {@code boolean}-typed result {@code Value}
     */
    public static Value isNull(BasicBlockBuilder bbb, Value value) {
        LiteralFactory lf = bbb.getLiteralFactory();
        return value.isNullable() ? bbb.isEq(value, lf.nullLiteralOfType(value.getType(NullableType.class))) : lf.literalOf(false);
    }

    /**
     * Emit a {@code null} check which throws {@code NullPointerException} when the given value is {@code null}.
     * If the value is known to be non-{@code null}, it is simply returned.
     *
     * @param bbb the basic block builder to use (must not be {@code null})
     * @param value the value to evaluate (must not be {@code null})
     * @return the {@code null}-checked value, which may be constrained as such compared to the original value (not {@code null})
     */
    public static Value checkNull(BasicBlockBuilder bbb, Value value) {
        if (value.isNullable()) {
            BlockLabel npe = new BlockLabel();
            BlockLabel resume = new BlockLabel();
            bbb.if_(isNull(bbb, value), npe, resume, Map.of(TEMP0, value));
            // throw an NPE
            bbb.begin(npe);
            throwException(bbb, RuntimeChecks.NPE, List.of());
            // null check passed OK
            bbb.begin(resume);
            return bbb.addParam(resume, TEMP0, value.getType(), false);
        } else {
            return value;
        }
    }

    /**
     * Throw an exception with the given class name and constructor arguments.
     *
     * @param bbb the basic block builder to use (must not be {@code null})
     * @param className the exception class name (must not be {@code null})
     * @param ctorArgs the arguments to pass to the constructor (must not be {@code null} but may be empty)
     * @return the block which was terminated by the exception throw (not {@code null})
     */
    public static BasicBlock throwException(BasicBlockBuilder bbb, String className, List<Value> ctorArgs) {
        return bbb.throw_(constructException(bbb, className, ctorArgs));
    }

    /**
     * Construct an exception with the given class name and constructor arguments.
     *
     * @param bbb the basic block builder to use (must not be {@code null})
     * @param className the exception class name (must not be {@code null})
     * @param ctorArgs the arguments to pass to the constructor (must not be {@code null} but may be empty)
     * @return the exception object (not {@code null})
     */
    public static Value constructException(final BasicBlockBuilder bbb, final String className, List<Value> ctorArgs) {
        CompilationContext ctxt = bbb.getContext();
        LiteralFactory lf = ctxt.getLiteralFactory();
        ClassContext bcc = ctxt.getBootstrapClassContext();
        LoadedTypeDefinition npeType = bcc.findDefinedType(className).load();
        StructType structType = Layout.get(ctxt).getInstanceLayoutInfo(npeType).getStructType();
        Value ex = bbb.new_(npeType.getClassType(), lf.literalOfType(npeType.getClassType()), lf.literalOf(structType.getSize()), lf.literalOf(structType.getAlign()));
        Value ctor = lf.literalOf(npeType.requireSingleConstructor(ce -> ce.getParameters().size() == 0));
        bbb.call(ctor, ex, ctorArgs);
        return ex;
    }
}
