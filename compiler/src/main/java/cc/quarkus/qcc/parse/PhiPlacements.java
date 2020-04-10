package cc.quarkus.qcc.parse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.type.Type;

public class PhiPlacements {

    public PhiPlacements() {

    }

    public void record(ControlNode<?> src, int index, Type type) {
        Set<Entry> set = this.indexes.get(src);
        if ( set == null ) {
            set = new HashSet<>();
            this.indexes.put(src, set);
        }
        set.add(new Entry(index, type));
    }

    public void forEach(BiConsumer<ControlNode<?>, Set<Entry>> fn) {
        this.indexes.forEach(fn);
    }

    private Map<ControlNode<?>, Set<Entry>> indexes = new HashMap<>();

    static class Entry {
        Entry(int index, Type type) {
            this.index = index;
            this.type = type;
        }

        final int index;
        final Type type;
    }
}
