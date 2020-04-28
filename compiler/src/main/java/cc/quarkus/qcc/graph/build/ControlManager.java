package cc.quarkus.qcc.graph.build;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.type.TypeDefinition;

public class ControlManager {

    public ControlManager(StartNode startNode) {
        recordControlForBci(-1, startNode);
        this.catchManager = new CatchManager(startNode.frame().maxLocals(), startNode.frame().maxStack());
    }

    public void recordControlForBci(int startBci, ControlNode<?> control) {
        this.controls.put(startBci, control);
    }

    public ControlNode<?> getControlForBci(int bci) {
        return this.controls.get(bci);
    }

    public void addControlInputForBci(int bci, ControlNode<?> input) {
        ControlNode<?> dest = getControlForBci(bci);
        if (dest == null) {
            dest = new RegionNode(input.frame().maxLocals(), input.frame().maxStack());
            recordControlForBci(bci, dest);
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
        worklist.add(getControlForBci(-1));

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

    private Map<Integer, ControlNode<?>> controls = new HashMap<>();

    private Map<ControlNode<?>, Set<ControlNode<?>>> dominanceFrontier;

    private CatchManager catchManager;
}
