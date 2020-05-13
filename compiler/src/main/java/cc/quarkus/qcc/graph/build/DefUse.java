package cc.quarkus.qcc.graph.build;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.type.TypeDescriptor;

public class DefUse {

    public DefUse(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public Entry def(ControlNode<?> src, int index, TypeDescriptor<?> type) {
        if (this.defs.stream().anyMatch(e -> (e.src == src) && (e.index == index))) {
            return null;
        }
        Entry entry = new Entry(src, index, type);
        this.defs.add(entry);
        return entry;
    }

    public void use(ControlNode<?> src, int index, TypeDescriptor<?> type) {
        //Set<Entry> set = this.uses.computeIfAbsent(src, k -> new HashSet<>());
        //set.add(new Entry(index, type));
        this.uses.add(new Entry(src, index, type));
    }

    public boolean isAliveAt(ControlNode<?> node, int index) {
        boolean result = this.uses.stream().anyMatch(e -> e.src == node && e.index == index);
        return result;
    }

    public void completePhis() {
        for (PhiLocal phi : this.phis) {
            phi.complete(frameManager());
        }
    }

    protected void placePhis() {
        Deque<Entry> worklist = new ArrayDeque<>(this.defs);

        while (!worklist.isEmpty()) {

            Entry entry = worklist.pop();

            Set<ControlNode<?>> df = nodeManager().dominanceFrontier(entry.src);

            for (ControlNode<?> dest : df) {
                if (isAliveAt(dest, entry.index)) {
                    this.phis.add(frameManager().of(dest).ensurePhi(entry.index, entry.src, entry.type));

                    Entry newEntry = def(dest, entry.index, entry.type);
                    if (newEntry != null) {
                        worklist.add(newEntry);
                    }
                }
            }
        }
    }

    protected NodeManager nodeManager() {
        return this.nodeManager;
    }

    protected FrameManager frameManager() {
        return nodeManager().frameManager();
    }

    private final NodeManager nodeManager;

    private List<Entry> defs = new ArrayList<>();

    private List<Entry> uses = new ArrayList<>();

    private Set<PhiLocal> phis = new HashSet<>();

    public static class Entry {
        Entry(ControlNode<?> src, int index, TypeDescriptor<?> type) {
            this.src = src;
            this.index = index;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "src=" + src +
                    ", index=" + index +
                    ", type=" + type +
                    '}';
        }

        final ControlNode<?> src;

        final int index;

        final TypeDescriptor<?> type;
    }

}
