package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.node.NewNode;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.TypeDefinition;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class NewTest extends AbstractNodeTestCase {

    @Test
    public void testNew() {
        TypeDefinition cls = getTypeDefinition();

        NewNode node = new NewNode( control(), cls.getTypeDescriptor());

        ObjectReference val = node.getValue(context());

        assertThat(val).isNotNull();
        assertThat( heap().allocated().contains(val));
        assertThat(val.getTypeDefinition()).isEqualTo(cls);
    }
}
