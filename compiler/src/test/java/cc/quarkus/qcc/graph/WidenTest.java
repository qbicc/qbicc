package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.node.NarrowNode;
import cc.quarkus.qcc.graph.node.WidenNode;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt8;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class WidenTest extends AbstractNodeTestCase {

    @Test
    public void testb2i() {
        MockNode<QInt8> in = set((byte) 42);
        WidenNode<QInt8, QInt32> node = WidenNode.b2i(graph(), control(), in);

        QInt32 result = node.getValue(context());
        assertThat(result.value()).isNotNull();
        assertThat(result.value()).isEqualTo(42);
    }
}

