package cc.quarkus.qcc.type.generic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class ClassMappingTests {
    @Test
    public void testSignatureFromClass() {
        final ParsingCache cache = new ParsingCache();
        final TypeSignature bats = TypeSignature.parseTypeSignature(cache, "[B");
        assertTrue(bats instanceof ArrayTypeSignature);
        assertSame(((ArrayTypeSignature) bats).getMemberSignature(), BaseTypeSignature.BYTE);
        assertSame(bats, TypeSignature.forClass(cache, byte[].class));
    }
}
