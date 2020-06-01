package cc.quarkus.qcc.constraint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class SatisfactionContextImpl implements SatisfactionContext {

    SatisfactionContextImpl(Value target) {
        this.target = target;
    }

    public Value getTarget() {
        return this.target;
    }

    @Override
    public Value getBinding(SymbolicValue symbol) {
        return this.bindings.get(symbol);
    }

    @Override
    public void bind(SymbolicValue symbol, Value value) {
        this.bindings.put(symbol, value);
    }

    @Override
    public Map<SymbolicValue, Value> getBindings() {
        return this.bindings;
    }

    @Override
    public void seen(Constraint c1, Constraint c2) {
        this.seen.add( new Entry(c1, c2));
    }

    @Override
    public boolean isSeen(Constraint c1, Constraint c2) {
        return this.seen.contains(new Entry(c1, c2));
    }

    private final Value target;
    private final Map<SymbolicValue, Value> bindings = new HashMap<>();

    private Set<Entry> seen = new HashSet<>();

    static class Entry {
        Entry(Constraint c1, Constraint c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return Objects.equals(c1, entry.c1) &&
                    Objects.equals(c2, entry.c2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(c1, c2);
        }

        private final Constraint c1;

        private final Constraint c2;
    }
}
