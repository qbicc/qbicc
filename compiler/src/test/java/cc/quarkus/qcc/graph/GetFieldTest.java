package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.node.GetFieldNode;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.definition.FieldDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class GetFieldTest extends AbstractNodeTestCase {

    private int someInt = 42;
    private Object someNull;

    @Test
    public void testGetPrimitive() {
        TypeDefinition cls = getTypeDefinition();
        FieldDefinition<QInt32> someInt = cls.findField("someInt");

        ObjectReference obj = cls.newInstance();
        MockNode<ObjectReference> objNode = set(obj);

        GetFieldNode<QInt32> node = new GetFieldNode<>(graph(), control(), objNode, someInt);
        QInt32 val = node.getValue(context());
        assertThat( val.value() ).isEqualTo(42);
    }

    @Test
    public void testGetNull() {
        TypeDefinition cls = getTypeDefinition();
        FieldDefinition<ObjectReference> someInt = cls.findField("someNull");

        ObjectReference obj = cls.newInstance();
        MockNode<ObjectReference> objNode = set(obj);

        GetFieldNode<ObjectReference> node = new GetFieldNode<>(graph(), control(), objNode, someInt);
        ObjectReference val = node.getValue(context());
        assertThat( val ).isNotNull();
        assertThat( val.isNull() ).isTrue();
    }
}
