package cc.quarkus.qcc.graph.build;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.type.QType;

public interface PhiData {

    Node<?> getValue(ControlNode<?> discriminator);

    PhiNode<?> getPhiNode();

    void complete(FrameManager frameManager);
}
