package org.qbicc.type.generic;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.qbicc.context.ClassContext;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class MethodSignatureParsingTests {
    static MethodSignature parse(ClassContext ctxt, String str) {
        return MethodSignature.parse(ctxt, ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testReallyComplex() {
        ClassContext ctxt = new TestClassContext();
        final MethodSignature mds = parse(ctxt, "<X:Y:TZ;>(Ljava/lang/Object;TX;TY;)V^Ljava/lang/Throwable;");
        assertEquals(2, mds.getTypeParameters().size());
        assertEquals("X", mds.getTypeParameters().get(0).getIdentifier());
        assertEquals("Y", mds.getTypeParameters().get(1).getIdentifier());
        assertTrue(mds.getTypeParameters().get(1).getClassBound() instanceof TypeVariableSignature);
        assertEquals("Z", ((TypeVariableSignature) mds.getTypeParameters().get(1).getClassBound()).getIdentifier());
        assertEquals(3, mds.getParameterTypes().size());
        assertEquals(SignatureParsingTests.parse(ctxt, "Ljava/lang/Object;"), mds.getParameterTypes().get(0));
        assertTrue(mds.getParameterTypes().get(1) instanceof TypeVariableSignature);
        assertEquals("X", ((TypeVariableSignature) mds.getParameterTypes().get(1)).getIdentifier());
        assertTrue(mds.getParameterTypes().get(2) instanceof TypeVariableSignature);
        assertEquals("Y", ((TypeVariableSignature) mds.getParameterTypes().get(2)).getIdentifier());
        assertEquals(BaseTypeSignature.V, mds.getReturnTypeSignature());
        assertEquals(1, mds.getThrowsSignatures().size());
        assertEquals(SignatureParsingTests.parse(ctxt, "Ljava/lang/Throwable;"), mds.getThrowsSignatures().get(0));
    }
}
