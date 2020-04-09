package cc.quarkus.qcc.metaprogram.jvm.signature;

import static org.junit.jupiter.api.Assertions.*;

import cc.quarkus.qcc.context.Context;
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
