package cc.quarkus.qcc.plugin.opt;

import java.util.Map;
import java.util.Set;

public class DUChain<D, U> {
    private Map<D, Set<U>> du;

    public DUChain(Map<D, Set<U>> du) {
        this.du = du;
    }

    public Set<U> getUseSet(D d) {
        return du.get(d);
    }

    public Set<D> getDefSet() {
        return du.keySet();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (D d : du.keySet()) {
            Set<U> uSet = du.get(d);
            if (uSet.size() == 0) {
                builder.append("[" + d + "]\n");
                continue;
            }
            for (U u : uSet) {
                builder.append("[" + d + ", " + u + "]\n");
            }
        }
        return builder.toString();
    }
}