package cc.quarkus.qcc.graph.build;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.CatchControlProjection;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.IOProjection;
import cc.quarkus.qcc.graph.node.MemoryProjection;
import cc.quarkus.qcc.graph.node.ParameterProjection;
import cc.quarkus.qcc.graph.node.Projection;
import cc.quarkus.qcc.graph.node.StartNode;
import cc.quarkus.qcc.graph.type.ExceptionProvider;
import cc.quarkus.qcc.graph.type.IOProvider;
import cc.quarkus.qcc.graph.type.MemoryProvider;
import cc.quarkus.qcc.type.MethodDescriptor;
import cc.quarkus.qcc.type.TypeDescriptor;

public class FrameManager {

    public FrameManager(Graph<?> graph) {
        this.graph = graph;

        StartNode start = graph.getStart();
        MethodDescriptor<?> descriptor = graph.getMethod();
        Frame startFrame = of(start);

        List<TypeDescriptor<?>> params = descriptor.getParamTypes();
        for (int i = 0; i < params.size(); ++i) {
            ParameterProjection<?> projection = new ParameterProjection<>(this.graph, start, params.get(i), i);
            startFrame.store(i, projection);
        }
        startFrame.io(new IOProjection(graph, start));
        startFrame.memory(new MemoryProjection(graph, start));
    }

    public Frame of(final ControlNode<?> control) {
        return this.frames.computeIfAbsent(control, c -> new Frame(c, getMaxLocals(), getMaxStack()));
    }

    protected void mergeInputs(ControlNode<?> control) {
        Frame frame = of(control);

        for (ControlNode<?> each : control.getControlPredecessors()) {
            Frame eachFrame = of(each);
            if ( control instanceof ExceptionProvider) {
                frame.mergeFrom( eachFrame, ((ExceptionProvider) control).getException());
            } else {
                frame.mergeFrom( eachFrame );
            }
        }

        for (ControlNode<?> each : control.getControlSuccessors()) {
            Frame eachFrame = of(each);
            if ( control instanceof IOProvider) {
                eachFrame.io(((IOProvider) control).getIO());
            }
            if ( control instanceof MemoryProvider) {
                eachFrame.memory(((MemoryProvider) control).getMemory());
            }
            if ( each instanceof Projection) {
                mergeInputs(each);
            }
        }
    }

    private int getMaxLocals() {
        return this.graph.getMethod().getMaxLocals();
    }

    public int getMaxStack() {
        return this.graph.getMethod().getMaxStack();
    }

    private final Graph<?> graph;

    private Map<ControlNode<?>, Frame> frames = new HashMap<>();
}
