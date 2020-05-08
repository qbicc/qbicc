package cc.quarkus.qcc.graph.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.TypeDefinition;

public class TryRange {

    public TryRange(Graph<?> graph, int startIndex, int endIndex, int maxLocals, int maxStack) {
        this.graph = graph;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public int width() {
        return this.endIndex - this.startIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TryRange tryRange = (TryRange) o;
        return startIndex == tryRange.startIndex &&
                endIndex == tryRange.endIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIndex, endIndex);
    }

    public CatchEntry addCatch(TypeDefinition type, int handlerIndex) {
        CatchEntry entry = getEntry(handlerIndex);
        if (type != null) {
            entry.getMatcher().addIncludeType(type);
            excludeFromOthers(entry, type);
            excludeOthersFrom(entry);
        }
        return entry;
    }

    public List<CatchEntry> getCatches() {
        return this.catches;
    }

    protected CatchEntry getEntry(int handlerIndex) {
        Optional<CatchEntry> result = this.catches.stream().filter(e -> e.handlerIndex == handlerIndex).findFirst();
        if (result.isPresent()) {
            return result.get();
        }

        RegionNode region = new RegionNode(this.graph, this.maxLocals, this.maxStack);
        CatchEntry entry = new CatchEntry(handlerIndex, region);
        this.catches.add(entry);
        return entry;
    }

    protected void excludeFromOthers(CatchEntry source, TypeDefinition type) {
        this.catches.forEach(e -> {
            if (e != source) {
                e.getMatcher().addExcludeType(type);
            }
        });
    }

    protected void excludeOthersFrom(CatchEntry source) {
        this.catches.forEach(e -> {
            if (e != source) {
                e.getMatcher().getIncludeTypes().forEach(t -> {
                    source.getMatcher().addExcludeType(t);
                });
            }
        });
    }

    private final int startIndex;

    private final int endIndex;

    private final int maxLocals;

    private final int maxStack;

    private final List<CatchEntry> catches = new ArrayList<>();

    private final Graph<?> graph;
}
