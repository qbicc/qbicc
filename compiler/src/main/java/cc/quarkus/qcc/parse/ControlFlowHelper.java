package cc.quarkus.qcc.parse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.graph.node.AbstractControlNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.StartNode;

public class ControlFlowHelper {

    public ControlFlowHelper(StartNode startNode) {
        control(-1, startNode);
    }

    public void control(int startBci, ControlNode<?,?> control) {
        this.controls.put(startBci, control);
    }

    public ControlNode<?,?> control(int bci) {
        return this.controls.get(bci);
    }

    public void add(int bci, ControlNode<?,?> input) {
        ControlNode<?,?> dest = control(bci);
        if (dest == null) {
            dest = new RegionNode(input.frame().maxLocals(), input.frame().maxStack());
            control(bci, dest);
        }
        if ( dest instanceof RegionNode ) {
            ((RegionNode)dest).addInput(input);
        } else {
            dest.setControl(input);
        }
    }

    public void dominanceFrontier(Map<ControlNode<?,?>, Set<ControlNode<?,?>>> dominanceFrontier) {
        this.dominanceFrontier = dominanceFrontier;
    }

    public Set<ControlNode<?,?>> dominanceFrontier(ControlNode<?,?> n) {
        return this.dominanceFrontier.get(n);
    }

    Set<ControlNode<?,?>> nodes() {
        Set<ControlNode<?,?>> nodes = new HashSet<>();
        Deque<ControlNode<?,?>> worklist = new ArrayDeque<>();
        worklist.add(control(-1));

        while ( ! worklist.isEmpty() ) {
            ControlNode<?,?> cur = worklist.pop();
            nodes.add(cur);
            for (ControlNode<?,?> successor : cur.getControlSuccessors()) {
                if ( ! nodes.contains(successor)) {
                    worklist.add(successor);
                }
            }
        }

        return nodes;
    }

    //private Map<Integer,List<ControlNode<?>>> links = new HashMap<>();
    private Map<Integer, ControlNode<?,?>> controls = new HashMap<>();
    private Map<ControlNode<?,?>, Set<ControlNode<?,?>>> dominanceFrontier;
}
