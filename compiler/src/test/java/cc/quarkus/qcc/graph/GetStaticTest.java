package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.AbstractTestCase;
import cc.quarkus.qcc.graph.node.GetStaticNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.type.FieldDefinition;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.TypeDescriptor;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class GetStaticTest extends AbstractTestCase {

    public static final int SomeInt = 42;

    @Test
    public void testGetStatic() {
        TypeDefinition cls = getTypeDefinition();
        FieldDefinition<Integer> someInt = cls.findField("SomeInt");

        assertThat( TypeDescriptor.INT ).isEqualTo(someInt.getTypeDescriptor());
        RegionNode control = new RegionNode(0,0);
        GetStaticNode<Integer> node = new GetStaticNode<>(control, someInt);

        Integer result = node.getValue(null);
        assertThat(result).isEqualTo(42);
    }
}
