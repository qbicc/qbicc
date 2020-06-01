package cc.quarkus.qcc.constraint;

import java.util.Map;

public interface SatisfactionContext {
    Value getTarget();
    Value getBinding(SymbolicValue symbol);
    void bind(SymbolicValue symbol, Value value);

    Map<SymbolicValue, Value> getBindings();

    void seen(Constraint c1, Constraint c2);
    boolean isSeen(Constraint c1, Constraint c2);
}
