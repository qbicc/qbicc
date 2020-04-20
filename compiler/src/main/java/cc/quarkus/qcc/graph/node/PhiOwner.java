package cc.quarkus.qcc.graph.node;

import java.util.List;

public interface PhiOwner {
    List<ControlNode<?, ?>> getInputs();
}
