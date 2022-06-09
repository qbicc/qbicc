package org.qbicc.graph;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.definition.element.ExecutableElement;

public final class GetAndSetMin extends AbstractReadModifyWriteValue {
    GetAndSetMin(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final ValueHandle target, final Value updateValue, ReadAccessMode readMode, WriteAccessMode writeMode) {
        super(callSite, element, line, bci, dependency, target, updateValue, readMode, writeMode);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "GetAndSetMin";
    }
}
