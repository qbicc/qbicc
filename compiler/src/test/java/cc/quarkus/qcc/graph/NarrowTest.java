package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.node.NarrowNode;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class NarrowTest extends AbstractNodeTestCase {

    @Test
    public void testi2b() {
        MockNode<Integer> in = set(42);
        NarrowNode<Integer, Byte> node = NarrowNode.i2b(graph(), control(), in);

        Byte result = node.getValue(context());
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo((byte) 42);
    }

    @Test
    public void testi2c() {
        MockNode<Integer> in = set(42);
        NarrowNode<Integer, Character> node = NarrowNode.i2c(graph(), control(), in);

        Character result = node.getValue(context());
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo('*');
    }

    @Test
    public void testi2s() {
        MockNode<Integer> in = set(42);
        NarrowNode<Integer, Short> node = NarrowNode.i2s(graph(), control(), in);

        Short result = node.getValue(context());
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo((short) 42);
    }

    @Test
    public void testl2i() {
        MockNode<Long> in = set(42L);
        NarrowNode<Long, Integer> node = NarrowNode.l2i(graph(), control(), in);

        Integer result = node.getValue(context());
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(42);
    }

    @Test
    public void testl2i_MoreThan32Bits() {
        MockNode<Long> in = set( 140737488355343L );
        NarrowNode<Long, Integer> node = NarrowNode.l2i(graph(), control(), in);

        Integer result = node.getValue(context());
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(15);
    }
}

