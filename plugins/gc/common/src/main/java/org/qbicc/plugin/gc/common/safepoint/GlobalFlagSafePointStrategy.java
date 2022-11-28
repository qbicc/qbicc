package org.qbicc.plugin.gc.common.safepoint;

import static org.qbicc.graph.atomic.AccessModes.SingleAcquire;
import static org.qbicc.graph.atomic.AccessModes.SingleRelease;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.definition.element.StaticMethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

/**
 * A safepoint polling strategy which uses a single global flag field to indicate that a safepoint should be entered.
 */
public final class GlobalFlagSafePointStrategy extends AbstractMethodBasedSafePointStrategy {

    private static final String REQUEST_SAFE_POINT_FIELD = "requestSafePoint";

    public GlobalFlagSafePointStrategy(CompilationContext ctxt) {
        super(ctxt);
        final Patcher patcher = Patcher.get(ctxt);
        FieldResolver requestSafePointResolver = (index, enclosing, builder) -> {
            builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.I_ACC_NO_REFLECT);
            builder.setEnclosingType(enclosing);
            builder.setSignature(TypeSignature.synthesize(enclosing.getContext(), builder.getDescriptor()));
            return builder.build();
        };
        patcher.addField(ctxt.getBootstrapClassContext(), SAFE_POINT_INT_NAME, REQUEST_SAFE_POINT_FIELD, BaseTypeDescriptor.Z, requestSafePointResolver, 0, 0);
    }

    private StaticFieldElement getField() {
        return ctxt.getBootstrapClassContext().findDefinedType(SAFE_POINT_INT_NAME).load().findStaticField(REQUEST_SAFE_POINT_FIELD);
    }

    @Override
    public void implementRequestGlobalSafePoint(BasicBlockBuilder bbb) {
        final LiteralFactory lf = bbb.getLiteralFactory();
        bbb.begin(new BlockLabel());
        bbb.store(lf.literalOf(getField()), lf.literalOf(true), SingleRelease);
        bbb.return_();
    }

    @Override
    public void implementClearGlobalSafePoint(BasicBlockBuilder bbb) {
        final LiteralFactory lf = bbb.getLiteralFactory();
        bbb.begin(new BlockLabel());
        bbb.store(lf.literalOf(getField()), lf.literalOf(false), SingleRelease);
        bbb.return_();
    }

    @Override
    public void implementPollSafePoint(BasicBlockBuilder bbb) {
        final LiteralFactory lf = bbb.getLiteralFactory();
        bbb.begin(new BlockLabel());
        final Value flag = bbb.load(lf.literalOf(getField()), SingleAcquire);
        BlockLabel enterSafePoint = new BlockLabel();
        BlockLabel resume = new BlockLabel();
        bbb.if_(flag, enterSafePoint, resume, Map.of());
        try {
            bbb.begin(enterSafePoint);
            final StaticMethodElement enterSafePointMethod = (StaticMethodElement) bbb.getContext().getBootstrapClassContext().findDefinedType(SAFE_POINT_INT_NAME).load().requireSingleMethod("enterSafePoint");
            // directly enter the safe point work method
            bbb.tailCall(bbb.staticMethod(enterSafePointMethod), List.of());
        } catch (BlockEarlyTermination ignored) {}
        try {
            bbb.begin(resume);
            bbb.return_();
        } catch (BlockEarlyTermination ignored) {}
    }
}
