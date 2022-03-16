package org.qbicc.machine.llvm.impl;

import java.io.IOException;

import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.debuginfo.DIGlobalVariableExpression;

final class DIGlobalVariableExpressionImpl extends AbstractMetadataNode implements DIGlobalVariableExpression {
    final AbstractValue var_;
    final AbstractValue expr;

    final AsValue value = new AsValue();

    DIGlobalVariableExpressionImpl(int index, AbstractValue var_, AbstractValue expr) {
        super(index);
        this.var_ = var_;
        this.expr = expr;
    }

    @Override
    public DIGlobalVariableExpression comment(String comment) {
        super.comment(comment);
        return this;
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        super.appendTo(target);
        value.appendTo(target);
        return target;
    }

    @Override
    public LLValue asValue() {
        return value;
    }

    final class AsValue extends AbstractValue {
        @Override
        public Appendable appendTo(Appendable target) throws IOException {
            target.append("!DIGlobalVariableExpression(");
            target.append("var: ");
            var_.appendTo(target);
            target.append(", expr: ");
            expr.appendTo(target);
            target.append(")");
            return target;
        }
    }

}
