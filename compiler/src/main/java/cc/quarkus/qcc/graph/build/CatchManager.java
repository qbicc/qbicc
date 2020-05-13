package cc.quarkus.qcc.graph.build;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.TypeDefinition;

public class CatchManager {

    public CatchManager(Graph<?> graph) {
        this.graph = graph;
    }

    public RegionNode addCatch(int startIndex, int endIndex, TypeDefinition type, int handlerIndex) {
        TryRange range = getRange(startIndex, endIndex);
        CatchEntry entry = range.addCatch(type, handlerIndex);
        return entry.getRegion();
    }

    public List<TryRange> getCatchesFor(int index) {
        return this.ranges.stream()
                .filter(e->e.getStartIndex() <= index && e.getEndIndex() > index)
                .sorted(Comparator.comparingInt(TryRange::width))
                .collect(Collectors.toList());
    }

    protected TryRange getRange(int startIndex, int endIndex) {
        Optional<TryRange> result = this.ranges.stream().filter(e -> e.getStartIndex() == startIndex && e.getEndIndex() == endIndex).findFirst();
        if ( result.isPresent() ) {
            return result.get();
        }

        TryRange range = new TryRange(this.graph, startIndex, endIndex);
        this.ranges.add( range );
        return range;
    }

    private final List<TryRange> ranges = new ArrayList<>();
    private final Graph<?> graph;
}
