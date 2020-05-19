package cc.quarkus.qcc.graph;

import java.io.IOException;

import cc.quarkus.qcc.AbstractTestCase;
import cc.quarkus.qcc.interpret.CallResult;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.definition.MethodDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import org.junit.Ignore;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class StackPhiTest extends AbstractTestCase {

    @Test
    public void testStackPhi() throws IOException {
        TypeDefinition type = getTypeDefinition();
        MethodDefinition<QInt32> m = (MethodDefinition<QInt32>) type.findMethod("stack", "(I)I");

        m.writeGraph( "target/graph.dot");

        CallResult<QInt32> result = m.call(QInt32.of(20));
        assertThat(result.getReturnValue()).isEqualTo(QInt32.of(88));

        result = m.call(QInt32.of(21));
        assertThat(result.getReturnValue()).isEqualTo(QInt32.of(42));
    }

    public static int stack(int arg) {
        return arg != 20 ? 42 : 88;
    }
}
