package cc.quarkus.qcc.graph.build;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.TypeDescriptor;

public class DefUse {

    public DefUse(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void def(ControlNode<?> src, int index, TypeDescriptor<?> type) {
        Set<Entry> set = this.defs.computeIfAbsent(src, k -> new HashSet<>());
        set.add(new Entry(index, type));
    }

    public void use(ControlNode<?> src, int index, TypeDescriptor<?> type) {
        Set<Entry> set = this.uses.computeIfAbsent(src, k -> new HashSet<>());
        set.add(new Entry(index, type));
    }

    public void forEach(BiConsumer<ControlNode<?>, Set<Entry>> fn) {
        this.defs.forEach(fn);
    }

    public void completePhis() {
        for (PhiLocal phi : this.phis) {
            phi.complete(frameManager());
        }
    }

    protected void placePhis() {
        forEach((node, entries) -> {
            if (!nodeManager().isReachable(node)) {
                return;
            }

            Set<ControlNode<?>> df = nodeManager().dominanceFrontier(node);

            //System.err.println( "DEF: " + node + " >> " + entries);

            for (DefUse.Entry entry : entries) {
                for (ControlNode<?> dest : df) {
                    //if (dest == getEndRegion() && (entry.index != SLOT_MEMORY && entry.index != SLOT_IO && entry.index != SLOT_COMPLETION)) {
                    //// skip
                    //continue;
                    //}
                    //System.err.println( "PLACE PHI: " + dest + " for " + entry.index);
                    this.phis.add(frameManager().of(dest).ensurePhi(entry.index, node, entry.type));
                }
            }
        });
    }

    protected NodeManager nodeManager() {
        return this.nodeManager;
    }

    protected FrameManager frameManager() {
        return nodeManager().frameManager();
    }

    private final NodeManager nodeManager;

    private Map<ControlNode<?>, Set<Entry>> defs = new HashMap<>();
    private Map<ControlNode<?>, Set<Entry>> uses = new HashMap<>();

    private Set<PhiLocal> phis = new HashSet<>();

    static class Entry {
        Entry(int index, TypeDescriptor<?> type) {
            this.index = index;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "index=" + index +
                    ", type=" + type +
                    '}';
        }

        final int index;
        final TypeDescriptor<?> type;
    }

}
