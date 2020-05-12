package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.node.NarrowNode;
import cc.quarkus.qcc.graph.node.WidenNode;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class WidenTest extends AbstractNodeTestCase {

    @Test
    public void testb2i() {
        MockNode<Byte> in = set( (byte) 42);
        WidenNode<Byte, Integer> node = WidenNode.b2i(graph(), control(), in);

        Integer result = node.getValue(context());
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(42);
    }
}

