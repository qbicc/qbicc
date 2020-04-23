package cc.quarkus.qcc.parse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.type.TypeDefinition;
import org.objectweb.asm.tree.InsnList;

public class ControlFlowHelper {

    public ControlFlowHelper(StartNode startNode) {
        control(-1, startNode);
    }

    public void control(int startBci, ControlNode<?> control) {
        this.controls.put(startBci, control);
    }

    public ControlNode<?> control(int bci) {
        return this.controls.get(bci);
    }

    public void add(int bci, ControlNode<?> input) {
        ControlNode<?> dest = control(bci);
        if (dest == null) {
            dest = new RegionNode(input.frame().maxLocals(), input.frame().maxStack());
            control(bci, dest);
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

    public void catches(TypeDefinition type, int startIndex, int endIndex, int handlerIndex, RegionNode catchRegion) {
        control(handlerIndex, catchRegion);
        this.catches.add(new CatchEntry(type, startIndex, endIndex, handlerIndex, catchRegion));
    }

    public List<CatchEntry> catchesFor(int bci) {
        //System.err.println( "GET CATCHES: " + bci + " for " + this.catches);
        return this.catches.stream()
                .filter(e -> e.startIndex <= bci && e.endIndex > bci)
                .sorted(Comparator.comparingInt(e -> e.endIndex - e.startIndex))
                .collect(Collectors.toList());
    }

    Set<ControlNode<?>> nodes() {
        Set<ControlNode<?>> nodes = new HashSet<>();
        Deque<ControlNode<?>> worklist = new ArrayDeque<>();
        worklist.add(control(-1));

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

    private List<CatchEntry> catches = new ArrayList<>();

    public static class CatchEntry {
        CatchEntry(TypeDefinition type, int startIndex, int endIndex, int handlerIndex, RegionNode handler) {
            this.type = type;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.handlerIndex = handlerIndex;
            this.handler = handler;
        }

        @Override
        public String toString() {
            return "CatchEntry{" +
                    "type=" + type +
                    ", startIndex=" + startIndex +
                    ", endIndex=" + endIndex +
                    ", handlerIndex=" + handlerIndex +
                    ", handler=" + handler +
                    '}';
        }

        final TypeDefinition type;

        final int startIndex;

        final int endIndex;

        final int handlerIndex;

        final RegionNode handler;

    }
}
