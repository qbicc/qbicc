package cc.quarkus.qcc.metaprogram.jvm.signature;

import static org.junit.jupiter.api.Assertions.*;

import cc.quarkus.qcc.context.Context;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DeclarationParsingTests {

    @Test
    public void testReallyComplex() {
        Context ctxt = new Context(false);
        ctxt.run(() -> {
            final ClassDeclarationSignature sig = ClassDeclarationSignature.parseClassDeclarationSignature("<Foo:Ljava/lang/String;:Ljava/lang/Iterable<TFoo;>;>Ljava/lang/Object;Ljava/lang/Iterable<Ljava/lang/Integer;>;");
            assertEquals(1, sig.getTypeParameterCount());
            assertTrue(sig.getTypeParameter(0).hasClassBound());
            assertEquals(TypeSignature.forClass(String.class), sig.getTypeParameter(0).getClassBound());
            assertEquals(1, sig.getTypeParameter(0).getInterfaceBoundCount());
            assertEquals(TypeSignature.forClass(Iterable.class), sig.getTypeParameter(0).getInterfaceBound(0).asClass().getRawType());
            assertEquals(1, sig.getTypeParameter(0).getInterfaceBound(0).asClass().getTypeArgumentCount());
            assertEquals("Foo", sig.getTypeParameter(0).getInterfaceBound(0).asClass().getTypeArgument(0).asBound().getValue().asTypeVariable().getSimpleName());
            assertEquals(TypeSignature.forClass(Object.class), sig.getSuperclass());
            assertEquals(1, sig.getInterfaceCount());
            assertEquals(TypeSignature.forClass(Iterable.class), sig.getInterface(0).getRawType());
            assertEquals(TypeSignature.forClass(Integer.class), sig.getInterface(0).getTypeArgument(0).asBound().getValue());
        });
    }
}
