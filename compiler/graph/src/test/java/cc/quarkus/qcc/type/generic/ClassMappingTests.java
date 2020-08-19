package cc.quarkus.qcc.type.generic;

import static org.junit.jupiter.api.Assertions.*;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.type.generic.ArrayTypeSignature;
import cc.quarkus.qcc.type.generic.BaseTypeSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ClassMappingTests {
    @Test
    public void testSignatureFromClass() {
        Context ctxt = new Context(false);
        ctxt.run(() -> {
            final TypeSignature bats = TypeSignature.parseTypeSignature("[B");
            assertTrue(bats instanceof ArrayTypeSignature);
            assertSame(((ArrayTypeSignature) bats).getMemberSignature(), BaseTypeSignature.BYTE);
            assertSame(bats, TypeSignature.forClass(byte[].class));
        });
    }
}
