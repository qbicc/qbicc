package cc.quarkus.qcc.graph.build;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class DefUse {

    public DefUse(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public LocalEntry defLocal(ControlNode<?> src, int index, TypeDescriptor<?> type) {
        if (this.localDefs.stream().anyMatch(e -> (e.src == src) && (e.index == index))) {
            return null;
        }
        LocalEntry entry = new LocalEntry(src, index, type);
        this.localDefs.add(entry);
        return entry;
    }

    public void useLocal(ControlNode<?> src, int index, TypeDescriptor<?> type) {
        this.localUses.add(new LocalEntry(src, index, type));
    }

    public boolean isLocalAliveAt(ControlNode<?> node, int index) {
        return this.localUses.stream().anyMatch(e -> e.src == node && e.index == index);
    }

    public void completePhis() {
        for (PhiData phi : this.phiData) {
            phi.complete(frameManager());
        }
    }

    public void push(ControlNode<?> control, TypeDescriptor<?> type) {
        StackEntry entry = this.stackEntries.computeIfAbsent(control, StackEntry::new);
        entry.push(type);
    }

    public void pop(ControlNode<?> control, TypeDescriptor<?> type) {
        StackEntry entry = this.stackEntries.computeIfAbsent(control, StackEntry::new);
        entry.pop(type);
    }

    protected void placePhis() {
        Deque<LocalEntry> worklist = new ArrayDeque<>(this.localDefs);

        while (!worklist.isEmpty()) {

            LocalEntry entry = worklist.pop();

            Set<ControlNode<?>> df = nodeManager().dominanceFrontier(entry.src);

            for (ControlNode<?> dest : df) {
                if (isLocalAliveAt(dest, entry.index)) {
                    this.phiData.add(frameManager().of(dest).ensurePhi(entry.index, entry.src, entry.type));

                    LocalEntry newEntry = defLocal(dest, entry.index, entry.type);
                    if (newEntry != null) {
                        worklist.add(newEntry);
                    }
                }
            }
        }

        calculateStackHeights();
        rectifyTails();

        this.stackEntries.forEach((control, entry) -> {
            List<TypeDescriptor<?>> defs = entry.defvar();
            Set<ControlNode<?>> df = nodeManager.dominanceFrontier(entry.src);
            for (ControlNode<?> dest : df) {
                this.phiData.addAll(frameManager().of(dest).ensureStackPhis(entry.src, defs, this.heights.get(dest) - defs.size()));
            }
        });
    }

    protected void rectifyTails() {
        this.stackEntries.forEach((control, entry) -> {
            if (control.getControlSuccessors().size() == 1) {
                Node<?> next = control.getControlSuccessors().get(0);
                if (next.getControlPredecessors().size() == 1) {
                    StackEntry nextEntries = this.stackEntries.get(next);
                    if ( nextEntries != null ) {
                        entry.rectify(this.stackEntries.get(next).uevar);
                    }
                }
            }
        });
    }

    protected void calculateStackHeights() {

        ControlNode<?> start = nodeManager.getControlForBci(-1);
        Deque<ControlNode<?>> worklist = new ArrayDeque<>();
        worklist.add(start);

        for (ControlNode<?> each : nodeManager().getAllControlNodes()) {
            this.heights.put(each, -1);
        }

        while (!worklist.isEmpty()) {
            ControlNode<?> cur = worklist.pop();
            int curHeight = this.heights.get(cur);

            int inboundHeight = 0;
            for (ControlNode<?> each : cur.getControlPredecessors()) {
                inboundHeight = Math.max(this.heights.get(each), inboundHeight);
            }
            StackEntry entry = this.stackEntries.get(cur);
            int newHeight = inboundHeight + (entry == null ? 0 : entry.defvar.size());
            if (newHeight != curHeight) {
                this.heights.put(cur, newHeight);
                worklist.addAll(cur.getControlSuccessors());
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

    private List<LocalEntry> localDefs = new ArrayList<>();

    private List<LocalEntry> localUses = new ArrayList<>();

    private Set<PhiData> phiData = new HashSet<>();

    Map<ControlNode<?>, Integer> heights = new HashMap<>();

    private Map<ControlNode<?>, StackEntry> stackEntries = new HashMap<>();

    public static class LocalEntry {
        LocalEntry(ControlNode<?> src, int index, TypeDescriptor<?> type) {
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

    private static class StackEntry {
        StackEntry(ControlNode<?> src) {
            this.src = src;
        }

        public void rectify(List<TypeDescriptor<?>> uevar) {
            for ( int i = uevar.size() - 1; i >= 0 ; --i) {
                pop(uevar.get(i).baseType());
            }
        }

        void push(TypeDescriptor<?> type) {
            this.defvar.push(type);
        }

        void pop(TypeDescriptor<?> type) {
            if (!this.defvar.isEmpty()) {
                TypeDescriptor<?> popped = this.defvar.pop();
                if (type == null) {
                    return;
                }
                assert type == popped : "attempted to pop " + type + " but received " + popped;
            } else {
                this.uevar.add(type);
            }
        }

        List<TypeDescriptor<?>> uevar() {
            return this.uevar;
        }

        List<TypeDescriptor<?>> defvar() {
            return new ArrayList<>(this.defvar);
        }

        @Override
        public String toString() {
            return "StackEntry{" +
                    "src=" + src +
                    ", uevar=" + uevar +
                    ", defvar=" + defvar +
                    '}';
        }

        private final ControlNode<?> src;

        private List<TypeDescriptor<?>> uevar = new ArrayList<>();

        private Deque<TypeDescriptor<?>> defvar = new ArrayDeque<>();
    }

}
