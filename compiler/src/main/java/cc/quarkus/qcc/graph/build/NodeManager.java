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
import cc.quarkus.qcc.type.definition.TypeDefinition;

public class NodeManager {

    public NodeManager(Graph<?> graph) {
        this.graph = graph;
        this.nodeFactory = new NodeFactory(this.graph);
        this.frameManager = new FrameManager(this.graph);
        this.catchManager = new CatchManager(graph);

        recordControlForBci(-1, graph.getStart());
    }

    public void recordControlForBci(int startBci, ControlNode<?> control) {
        List<ControlNode<?>> chain = this.controls.computeIfAbsent(startBci, k -> new ArrayList<>());
        chain.add(control);
    }

    public ControlNode<?> getControlForBci(int bci) {
        List<ControlNode<?>> chain = getControlChainForBci(bci);
        if (chain == null) {
            return null;
        }

        return chain.get(chain.size() - 1);
    }

    public List<ControlNode<?>> getControlChainForBci(int bci) {
        return this.controls.get(bci);
    }

    public void addControlInputForBci(int bci, ControlNode<?> input) {
        List<ControlNode<?>> chain = getControlChainForBci(bci);
        ControlNode<?> dest = null;
        if (chain == null) {
            dest = nodeFactory.regionNode();
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

    public NodeFactory nodeFactory() {
        return this.nodeFactory;
    }

    public FrameManager frameManager() {
        return this.frameManager;
    }

    public void setControl(ControlNode<?> control) {
        this.currentControl = control;
        this.nodeFactory().setControl(control);
    }

    public void setControl(List<ControlNode<?>> chain) {
        setControl(chain.get(chain.size() - 1));
    }

    public ControlNode<?> currentControl() {
        return this.currentControl;
    }

    public void clearControl() {
        this.currentControl = null;
    }

    public void initializeControlForStructure(int bci) {
        List<ControlNode<?>> chain = getControlChainForBci(bci);
        if (chain != null) {
            if (currentControl() != null) {
                if (chain.get(0) instanceof RegionNode) {
                    ((RegionNode) chain.get(0)).addInput(currentControl());
                } else {
                    if (chain.get(0).getControl() == null) {
                        chain.get(0).setControl(currentControl());
                    }
                }
            }
            setControl(chain);
        }
    }

    public void initializeControlForInstructions(int bci) {
        List<ControlNode<?>> chain = getControlChainForBci(bci);

        if (chain != null) {
            for (ControlNode<?> each : chain) {
                mergeInputs(each);
            }
            setControl(chain);
        }
    }

    public void finalizeControlForInstructions(int bci) {
        List<ControlNode<?>> chain = getControlChainForBci(bci);
        if ( chain != null ) {
            mergeOutputs(chain.get(chain.size()-1));
        }
    }

    protected void mergeInputs(ControlNode<?> control) {
        frameManager().mergeInputs(control);
    }

    protected void mergeOutputs(ControlNode<?> control) {
        frameManager().mergeOutputs(control);
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

    private final NodeFactory nodeFactory;

    private final FrameManager frameManager;

    private Map<Integer, List<ControlNode<?>>> controls = new HashMap<>();

    private Map<ControlNode<?>, Set<ControlNode<?>>> dominanceFrontier;

    private CatchManager catchManager;

    private ControlNode<?> currentControl;
}
