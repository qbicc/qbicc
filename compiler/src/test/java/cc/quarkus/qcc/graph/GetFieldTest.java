package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.node.GetFieldNode;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.FieldDefinition;
import cc.quarkus.qcc.type.TypeDefinition;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class GetFieldTest extends AbstractNodeTestCase {

    private int someInt = 42;

    @Test
    public void testGet() {
        TypeDefinition cls = getTypeDefinition();
        FieldDefinition<Integer> someInt = cls.findField("someInt");

        ObjectReference obj = cls.newInstance();
        MockNode<ObjectReference> objNode = set(obj);

        GetFieldNode<Integer> node = new GetFieldNode<>(control(), objNode, someInt);
        Integer val = node.getValue(context());
        assertThat( val ).isEqualTo(42);
    }

}
