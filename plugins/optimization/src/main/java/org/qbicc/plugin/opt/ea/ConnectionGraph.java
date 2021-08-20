package org.qbicc.plugin.opt.ea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;

// TODO override toString() and show the name of the method for which this connection graph is set
final class ConnectionGraph {
    private final Map<Node, Value> pointsToEdges = new HashMap<>(); // solid (P) edges
    private final Map<Node, ValueHandle> deferredEdges = new HashMap<>(); // dashed (D) edges
    private final Map<Value, Collection<InstanceFieldOf>> fieldEdges = new HashMap<>(); // solid (F) edges

    Collection<InstanceFieldOf> getFieldEdges(Value value) {
        return fieldEdges.get(value);
    }

    /**
     * PointsTo(p) returns the set of of nodes that are immediately pointed by p.
     */
    Collection<Value> getPointsTo(Node node) {
        // TODO do deferred need to be taken into account? Shouldn't ByPass have removed them?

        final ValueHandle deferredEdge = deferredEdges.get(node);
        if (deferredEdge != null) {
            final Value pointedByDeferred = pointsToEdges.get(deferredEdge);
            if (pointedByDeferred != null)
                return List.of(pointedByDeferred);
        }

        final Value pointedDirectly = pointsToEdges.get(node);
        if (pointedDirectly != null) {
            return List.of(pointedDirectly);
        }

        return Collections.emptyList();
    }
    
    boolean addFieldEdgeIfAbsent(New from, InstanceFieldOf to) {
        return fieldEdges
            .computeIfAbsent(from, obj -> new ArrayList<>())
            .add(to);
    }

    boolean addPointsToEdgeIfAbsent(ValueHandle from, New to) {
        return pointsToEdges.putIfAbsent(from, to) == null;
    }

    boolean addDeferredEdgeIfAbsent(Node from, ValueHandle to) {
        return deferredEdges.putIfAbsent(from, to) == null;
    }

    List<Node> getReachableFrom(Node from) {
        final List<Node> reachable = new ArrayList<>();
        getReachableFrom(from, reachable);
        return reachable;
    }

    private void getReachableFrom(Node from, List<Node> reachable) {
        final Node to = deferredEdges.get(from) != null
            ? deferredEdges.get(from)
            : pointsToEdges.get(from);

        if (to != null) {
            reachable.add(to);
            getReachableFrom(to, reachable);
        }
    }
}
