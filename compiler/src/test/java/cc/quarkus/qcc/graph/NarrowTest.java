package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.node.NarrowNode;
import cc.quarkus.qcc.type.QChar;
import cc.quarkus.qcc.type.QInt16;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.QInt8;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class NarrowTest extends AbstractNodeTestCase {

    @Test
    public void testi2b() {
        MockNode<QInt32> in = set(42);
        NarrowNode<QInt32, QInt8> node = NarrowNode.i2b(graph(), control(), in);

        QInt8 result = node.getValue(context());
        assertThat(result.value()).isNotNull();
        assertThat(result.value()).isEqualTo((byte) 42);
    }

    @Test
    public void testi2c() {
        MockNode<QInt32> in = set(42);
        NarrowNode<QInt32, QChar> node = NarrowNode.i2c(graph(), control(), in);

        QChar result = node.getValue(context());
        assertThat(result.value()).isNotNull();
        assertThat(result.value()).isEqualTo('*');
    }

    @Test
    public void testi2s() {
        MockNode<QInt32> in = set(42);
        NarrowNode<QInt32, QInt16> node = NarrowNode.i2s(graph(), control(), in);

        QInt16 result = node.getValue(context());
        assertThat(result.value()).isNotNull();
        assertThat(result.value()).isEqualTo((short) 42);
    }

    @Test
    public void testl2i() {
        MockNode<QInt64> in = set(42L);
        NarrowNode<QInt64, QInt32> node = NarrowNode.l2i(graph(), control(), in);

        QInt32 result = node.getValue(context());
        assertThat(result.value()).isNotNull();
        assertThat(result.value()).isEqualTo(42);
    }

    @Test
    public void testl2i_MoreThan32Bits() {
        MockNode<QInt64> in = set( 140737488355343L );
        NarrowNode<QInt64, QInt32> node = NarrowNode.l2i(graph(), control(), in);

        QInt32 result = node.getValue(context());
        assertThat(result.value()).isNotNull();
        assertThat(result.value()).isEqualTo(15);
    }
}

