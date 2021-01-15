package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

public final class GetAndSet extends AbstractReadModifyWriteValue {
    GetAndSet(final Node callSite, final ExecutableElement  element, final int line, final int bci, final Node dependency, final ValueHandle target, final Value updateValue, final MemoryAtomicityMode atomicityMode) {
        super(callSite, element, line, bci, dependency, target, updateValue, atomicityMode);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
