package cc.quarkus.qcc.type.generic;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class SignatureParsingTests {

    static final String JLO_SIG_STR = "L" + "java/lang/Object;";

    @Test
    public void testBasicTypes() {
        final ParsingCache cache = new ParsingCache();
        assertEquals(TypeSignature.parseTypeSignature(cache, "B"), BaseTypeSignature.BYTE);
        assertEquals(TypeSignature.parseTypeSignature(cache, "S"), BaseTypeSignature.SHORT);
        assertEquals(TypeSignature.parseTypeSignature(cache, "I"), BaseTypeSignature.INT);
        assertEquals(TypeSignature.parseTypeSignature(cache, "J"), BaseTypeSignature.LONG);
        assertEquals(TypeSignature.parseTypeSignature(cache, "C"), BaseTypeSignature.CHAR);
        assertEquals(TypeSignature.parseTypeSignature(cache, "Z"), BaseTypeSignature.BOOLEAN);
        assertEquals(TypeSignature.parseTypeSignature(cache, "F"), BaseTypeSignature.FLOAT);
        assertEquals(TypeSignature.parseTypeSignature(cache, "D"), BaseTypeSignature.DOUBLE);
    }

    @Test
    public void testReferenceTypes() {
        final ParsingCache cache = new ParsingCache();
        final TypeSignature sig = TypeSignature.parseTypeSignature(cache, JLO_SIG_STR);
        assertTrue(sig.isClass());
        assertEquals("Object", sig.asClass().getSimpleName());
        assertEquals("lang", sig.asClass().getPackageName().getSimpleName());
        assertEquals("java", sig.asClass().getPackageName().getEnclosing().getSimpleName());
        assertSame(sig, TypeSignature.parseTypeSignature(cache, JLO_SIG_STR));
    }

    @Test
    public void testSimpleTypeVariable() {
        final ParsingCache cache = new ParsingCache();
        final TypeSignature sig = TypeSignature.parseTypeSignature(cache, "TFoo;");
        assertTrue(sig.isTypeVariable());
        assertEquals("Foo", sig.asTypeVariable().getSimpleName());
        assertSame(sig.asTypeVariable(), TypeSignature.parseTypeSignature(cache, "TFoo;"));
    }

    @Test
    public void testArrayOfSimpleType() {
        final ParsingCache cache = new ParsingCache();
        final TypeSignature sig = TypeSignature.parseTypeSignature(cache, "[B");
        assertTrue(sig.isArray());
        assertEquals(BaseTypeSignature.BYTE, sig.asArray().getMemberSignature());
        assertSame(sig, TypeSignature.parseTypeSignature(cache, "[B"));
    }

    @Test
    public void testArrayOfArrayOfSimpleType() {
        final ParsingCache cache = new ParsingCache();
        final TypeSignature sig = TypeSignature.parseTypeSignature(cache, "[[J");
        assertTrue(sig.isArray());
        assertTrue(sig.asArray().getMemberSignature().isArray());
        assertEquals(BaseTypeSignature.LONG, sig.asArray().getMemberSignature().asArray().getMemberSignature());
        assertSame(sig, TypeSignature.parseTypeSignature(cache, "[[J"));
    }

    @Test
    public void testReallyComplex() {
        final ParsingCache cache = new ParsingCache();
        final TypeSignature sig = TypeSignature.parseTypeSignature(cache, "Lfoo/bar/baz/Zap<**+[TT;-TT;Lblah/Bzzt;*>.Zap<-TXxxxxx;>;;");
        assertTrue(sig.isClass());
        assertTrue(sig.asClass().hasEnclosing());
        final ClassTypeSignature outer = sig.asClass().getEnclosing();
        assertEquals(6, outer.getTypeArgumentCount());
        assertSame(outer.getTypeArgument(0), AnyTypeArgument.INSTANCE);
        assertSame(outer.getTypeArgument(1), AnyTypeArgument.INSTANCE);
        final TypeArgument arg2 = outer.getTypeArgument(2);
        assertTrue(arg2.isBound());
        assertEquals(Variance.COVARIANT, arg2.asBound().getVariance());
        final ReferenceTypeSignature bta2Type = arg2.asBound().getValue();
        assertTrue(bta2Type.isArray());
        final TypeSignature bta2TypeType = bta2Type.asArray().getMemberSignature();
        assertTrue(bta2TypeType.isTypeVariable());
        assertEquals("T", bta2TypeType.asTypeVariable().getSimpleName());
        final TypeArgument arg3 = outer.getTypeArgument(3);
        assertTrue(arg3.isBound());
        assertEquals(Variance.CONTRAVARIANT, arg3.asBound().getVariance());
        final ReferenceTypeSignature bta3Type = arg3.asBound().getValue();
        assertTrue(bta3Type.isTypeVariable());
        assertSame(bta2TypeType, bta3Type);
        final TypeArgument arg4 = outer.getTypeArgument(4);
        assertTrue(arg4.isBound());
        assertEquals(Variance.INVARIANT, arg4.asBound().getVariance());
        assertEquals("Bzzt", arg4.asBound().getValue().asClass().getSimpleName());
        assertSame(outer.getTypeArgument(5), AnyTypeArgument.INSTANCE);
        assertSame(outer.getSimpleName(), sig.asClass().getSimpleName());
        assertEquals(1, sig.asClass().getTypeArgumentCount());
    }
}
