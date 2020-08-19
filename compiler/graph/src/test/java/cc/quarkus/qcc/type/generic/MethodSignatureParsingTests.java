package cc.quarkus.qcc.type.generic;

import static org.junit.jupiter.api.Assertions.*;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.type.generic.MethodDeclarationSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;

/**
 *
 */
public class MethodSignatureParsingTests {

    public void testReallyComplex() {
        Context ctxt = new Context(false);
        ctxt.run(() -> {
            final MethodDeclarationSignature mds = MethodDeclarationSignature.parseMethodDeclarationSignature("<TX:;TY:TX;;>(Ljava/lang/Object;TX;TY;)V^Ljava/lang/Throwable;");
            assertEquals(2, mds.getTypeParameterCount());
            assertEquals("X", mds.getTypeParameter(0).getSimpleName());
            assertEquals("Y", mds.getTypeParameter(1).getSimpleName());
            assertTrue(mds.getTypeParameter(0).getClassBound().isTypeVariable());
            assertEquals("X", mds.getTypeParameter(0).getClassBound().asTypeVariable().getSimpleName());
            assertEquals(3, mds.getParameterCount());
            assertEquals(TypeSignature.forClass(Object.class), mds.getParameterType(0));
            assertEquals("X", mds.getParameterType(1).asTypeVariable().getSimpleName());
            assertEquals("Y", mds.getParameterType(2).asTypeVariable().getSimpleName());
            assertFalse(mds.hasReturnType());
            assertEquals(1, mds.getThrowsCount());
            assertEquals(TypeSignature.forClass(Throwable.class), mds.getThrowsType(0));
        });
    }
}
