package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.node.GetStaticNode;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.definition.FieldDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class GetStaticTest extends AbstractNodeTestCase {

    public static final int SomeInt = 42;

    @Test
    public void testGetStatic() {
        TypeDefinition cls = getTypeDefinition();
        FieldDefinition<QInt32> someInt = cls.findField("SomeInt");

        assertThat( TypeDescriptor.INT32).isEqualTo(someInt.getTypeDescriptor());
        GetStaticNode<QInt32> node = new GetStaticNode<>(graph(), control(), someInt);

        QInt32 result = node.getValue(null);
        assertThat(result.value()).isEqualTo(42);
    }
}
