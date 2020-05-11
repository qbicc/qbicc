package cc.quarkus.qcc.graph.build;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.TypeDefinition;

public class ControlManager {

    public ControlManager(Graph<?> graph) {
        this.graph = graph;
        recordControlForBci(-1, graph.getStart());
        this.catchManager = new CatchManager(graph);
    }

    public void recordControlForBci(int startBci, ControlNode<?> control) {
        List<ControlNode<?>> chain = this.controls.computeIfAbsent(startBci, k -> new ArrayList<>());
        chain.add(control);
    }

    public ControlNode<?> getControlForBci(int bci) {
        List<ControlNode<?>> chain = getControlChainForBci(bci);
        if ( chain == null ) {
            return null;
        }

        return chain.get(chain.size()-1);
    }

    public List<ControlNode<?>> getControlChainForBci(int bci) {
        return this.controls.get(bci);
    }

    public void addControlInputForBci(int bci, ControlNode<?> input) {
        List<ControlNode<?>> chain = getControlChainForBci(bci);
        ControlNode<?> dest = null;
        if (chain == null) {
            dest = new RegionNode(this.graph, input.frame().maxLocals(), input.frame().maxStack());
            recordControlForBci(bci, dest);
        } else {
            dest = chain.get(0);
        }
        if (dest instanceof RegionNode) {
            ((RegionNode) dest).addInput(input);
        } else {
            dest.setControl(input);
        }
    }

    public void dominanceFrontier(Map<ControlNode<?>, Set<ControlNode<?>>> dominanceFrontier) {
        this.dominanceFrontier = dominanceFrontier;
    }

    public Set<ControlNode<?>> dominanceFrontier(ControlNode<?> n) {
        return this.dominanceFrontier.get(n);
    }

    public void addCatch(TypeDefinition type, int startIndex, int endIndex, int handlerIndex) {
        RegionNode catchRegion = this.catchManager.addCatch(startIndex, endIndex, type, handlerIndex);
        recordControlForBci(handlerIndex, catchRegion);
    }

    public List<TryRange> getCatchesForBci(int bci) {
        return this.catchManager.getCatchesFor(bci);
    }

    Set<ControlNode<?>> getAllControlNodes() {
        Set<ControlNode<?>> nodes = new HashSet<>();
        Deque<ControlNode<?>> worklist = new ArrayDeque<>();
        worklist.addAll(getControlChainForBci(-1));

        while (!worklist.isEmpty()) {
            ControlNode<?> cur = worklist.pop();
            nodes.add(cur);
            for (ControlNode<?> successor : cur.getControlSuccessors()) {
                if (!nodes.contains(successor)) {
                    worklist.add(successor);
                }
            }
        }

        return nodes;
    }

    private final Graph<?> graph;

    private Map<Integer, List<ControlNode<?>>> controls = new HashMap<>();

    private Map<ControlNode<?>, Set<ControlNode<?>>> dominanceFrontier;

    private CatchManager catchManager;

}
