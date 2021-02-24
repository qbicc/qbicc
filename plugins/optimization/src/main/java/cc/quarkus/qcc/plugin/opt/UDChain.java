package cc.quarkus.qcc.plugin.opt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BlockEntry;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.OrderedNode;

public class UDChain<U, D> {
    private Map<U, Set<D>> ud;

    public UDChain() {
        ud = new HashMap<U, Set<D>>();
    }

    private void addUse(U u) {
        assert !ud.containsKey(u);
        ud.put(u, new HashSet<D>());
    }

    public void addDef(D d, U u) {
        Set<D> set = ud.get(u);
        assert set != null;
        set.add(d);
    }

    public Set<U> getUseSet() {
        return ud.keySet();
    }

    public Set<D> getDefSet(U u) {
        return ud.get(u);
    }

    /**
     * Construct UD chain.
     * @param currentUse use of UD chain
     * @param addD function to record def under given conditions
     */
    public void constructUD(U currentUse, BiFunction<U, Node, Boolean> addD) {
        addUse(currentUse);
        constructUD(((OrderedNode) currentUse).getDependency(), currentUse, addD, new ArrayList<Node>());
    }

    private void constructUD(Node node, U currentUse, BiFunction<U, Node, Boolean> addD, List<Node> trace) {
        if (trace.contains(node)) {
            return;
        }
        trace.add(node);

        if (node instanceof BlockEntry) {
            // A basic block ends. Look for parent basic blocks.
            BlockEntry entry = (BlockEntry) node;
            BlockLabel label = entry.getPinnedBlockLabel();
            BasicBlock block = BlockLabel.getTargetOf(label);
            for (BasicBlock b : block.getIncoming()) {
                constructUD(b.getTerminator(), currentUse, addD, new ArrayList<Node>(trace));
            }
            return;
        }

        // Expect to record def for currentUse
        if (addD.apply(currentUse, node)) {
            return;
        }

        constructUD(((OrderedNode) node).getDependency(), currentUse, addD, trace);
    }

    /**
     * Create DU chain based on UD chain. Obtained DU chain misses the defs
     * which are not used, but such defs are ignorable.
     */
    public DUChain<D, U> constructDU() {
        Map<D, Set<U>> du = new HashMap<D, Set<U>>();
        Set<U> uKeySet = ud.keySet();
        for (U u : uKeySet) {
            Set<D> dSet = ud.get(u);
            for (D d : dSet) {
                Set<U> uSet = du.containsKey(d) ? du.get(d) : new HashSet<U>();
                uSet.add(u);
                du.put(d, uSet);
            }
        }

        return new DUChain<D, U>(du);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (U u : ud.keySet()) {
            Set<D> dSet = ud.get(u);
            if (dSet.size() == 0) {
                builder.append("[" + u + "]\n");
                continue;
            }
            for (D d : dSet) {
                builder.append("[" + u + ", " + d + "]\n");
            }
        }
        return builder.toString();
    }
}