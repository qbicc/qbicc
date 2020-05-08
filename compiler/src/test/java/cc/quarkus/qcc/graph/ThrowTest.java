package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.AbstractTestCase;
import cc.quarkus.qcc.graph.node.GetStaticNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.node.ThrowNode;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.ThrowToken;
import cc.quarkus.qcc.type.FieldDefinition;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.TypeDescriptor;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class ThrowTest extends AbstractNodeTestCase {

    @Test
    public void testProjections() {
        TypeDefinition exCls = getTypeDefinition(Exception.class);
        ObjectReference ex = exCls.newInstance();

        Node<ObjectReference> thrown = set(ex);
        ThrowNode node = new ThrowNode(graph(), control());
        node.setThrown(thrown);

        ThrowToken token = node.getValue(context());
        assertThat( token ).isNotNull();
        assertThat( token.getThrowValue() ).isSameAs(ex);

        ControlToken ctrl = node.getThrowControlOut().getValue(context());
        assertThat( ctrl ).isNotNull();
        assertThat( ctrl ).isNotSameAs(ControlToken.NO_CONTROL);
    }
}
