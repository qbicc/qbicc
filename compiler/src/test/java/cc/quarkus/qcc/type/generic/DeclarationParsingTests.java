package cc.quarkus.qcc.type.generic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class DeclarationParsingTests {

    @Test
    public void testReallyComplex() {
        final ParsingCache cache = new ParsingCache();
        final ClassDeclarationSignature sig = ClassDeclarationSignature.parseClassDeclarationSignature(cache, "<Foo:Ljava/lang/String;:Ljava/lang/Iterable<TFoo;>;>Ljava/lang/Object;Ljava/lang/Iterable<Ljava/lang/Integer;>;");
        assertEquals(1, sig.getTypeParameterCount());
        assertTrue(sig.getTypeParameter(0).hasClassBound());
        assertEquals(TypeSignature.forClass(cache, String.class), sig.getTypeParameter(0).getClassBound());
        assertEquals(1, sig.getTypeParameter(0).getInterfaceBoundCount());
        assertEquals(TypeSignature.forClass(cache, Iterable.class), sig.getTypeParameter(0).getInterfaceBound(0).asClass().getRawType());
        assertEquals(1, sig.getTypeParameter(0).getInterfaceBound(0).asClass().getTypeArgumentCount());
        assertEquals("Foo", sig.getTypeParameter(0).getInterfaceBound(0).asClass().getTypeArgument(0).asBound().getValue().asTypeVariable().getSimpleName());
        assertEquals(TypeSignature.forClass(cache, Object.class), sig.getSuperclass());
        assertEquals(1, sig.getInterfaceCount());
        assertEquals(TypeSignature.forClass(cache, Iterable.class), sig.getInterface(0).getRawType());
        assertEquals(TypeSignature.forClass(cache, Integer.class), sig.getInterface(0).getTypeArgument(0).asBound().getValue());
    }
}
