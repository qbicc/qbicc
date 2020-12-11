package cc.quarkus.qcc.type.generic;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import cc.quarkus.qcc.type.definition.ClassContext;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DeclarationParsingTests {
    static ClassSignature parse(ClassContext ctxt, String str) {
        return ClassSignature.parse(ctxt, ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testReallyComplex() {
        ClassContext ctxt = new TestClassContext();
        final ClassSignature sig = parse(ctxt, "<Foo:Ljava/lang/String;:Ljava/lang/Iterable<TFoo;>;>Ljava/lang/Object;Ljava/lang/Iterable<Ljava/lang/Integer;>;");
        assertEquals(1, sig.getTypeParameters().size());
        assertNotNull(sig.getTypeParameters().get(0).getClassBound());
        assertEquals(SignatureParsingTests.parse(ctxt, "Ljava/lang/String;"), sig.getTypeParameters().get(0).getClassBound());
        assertEquals(1, sig.getTypeParameters().get(0).getInterfaceBounds().size());
        assertTrue(sig.getTypeParameters().get(0).getInterfaceBounds().get(0) instanceof ClassTypeSignature);
        assertEquals(SignatureParsingTests.parse(ctxt, "Ljava/lang/Iterable<TFoo;>;"), sig.getTypeParameters().get(0).getInterfaceBounds().get(0));
        assertEquals(1, ((ClassTypeSignature) sig.getTypeParameters().get(0).getInterfaceBounds().get(0)).getTypeArguments().size());
        assertTrue(((ClassTypeSignature)sig.getTypeParameters().get(0).getInterfaceBounds().get(0)).getTypeArguments().get(0) instanceof BoundTypeArgument);
        assertTrue(((BoundTypeArgument) ((ClassTypeSignature) sig.getTypeParameters().get(0).getInterfaceBounds().get(0)).getTypeArguments().get(0)).getBound() instanceof TypeVariableSignature);
        assertEquals("Foo", ((TypeVariableSignature) ((BoundTypeArgument) ((ClassTypeSignature) sig.getTypeParameters().get(0).getInterfaceBounds().get(0)).getTypeArguments().get(0)).getBound()).getIdentifier());
        assertEquals(SignatureParsingTests.parse(ctxt, "Ljava/lang/Object;"), sig.getSuperClassSignature());
        assertEquals(1, sig.getInterfaceSignatures().size());
        assertEquals(SignatureParsingTests.parse(ctxt, "Ljava/lang/Iterable<Ljava/lang/Integer;>;"), sig.getInterfaceSignatures().get(0));
        assertTrue(sig.getInterfaceSignatures().get(0).getTypeArguments().get(0) instanceof BoundTypeArgument);
        assertEquals(SignatureParsingTests.parse(ctxt, "Ljava/lang/Integer;"), ((BoundTypeArgument) sig.getInterfaceSignatures().get(0).getTypeArguments().get(0)).getBound());
    }
}
