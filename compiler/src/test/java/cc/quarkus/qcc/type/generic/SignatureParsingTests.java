package org.qbicc.type.generic;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.qbicc.type.definition.ClassContext;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class SignatureParsingTests {

    static final String JLO_SIG_STR = "L" + "java/lang/Object;";

    static TypeSignature parse(ClassContext ctxt, String str) {
        return TypeSignature.parse(ctxt, ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testBasicTypes() {
        ClassContext ctxt = new TestClassContext();
        assertEquals(parse(ctxt, "B"), BaseTypeSignature.B);
        assertEquals(parse(ctxt, "C"), BaseTypeSignature.C);
        assertEquals(parse(ctxt, "D"), BaseTypeSignature.D);
        assertEquals(parse(ctxt, "F"), BaseTypeSignature.F);
        assertEquals(parse(ctxt, "I"), BaseTypeSignature.I);
        assertEquals(parse(ctxt, "J"), BaseTypeSignature.J);
        assertEquals(parse(ctxt, "S"), BaseTypeSignature.S);
        assertEquals(parse(ctxt, "V"), BaseTypeSignature.V);
        assertEquals(parse(ctxt, "Z"), BaseTypeSignature.Z);
    }

    @Test
    public void testReferenceTypes() {
        ClassContext ctxt = new TestClassContext();
        final TypeSignature sig = parse(ctxt, JLO_SIG_STR);
        assertTrue(sig instanceof TopLevelClassTypeSignature);
        assertEquals("Object", ((TopLevelClassTypeSignature) sig).getIdentifier());
        assertEquals("java/lang", ((TopLevelClassTypeSignature) sig).getPackageName());
        assertSame(sig, parse(ctxt, JLO_SIG_STR));
    }

    @Test
    public void testSimpleTypeVariable() {
        ClassContext ctxt = new TestClassContext();
        final TypeSignature sig = parse(ctxt, "TFoo;");
        assertTrue(sig instanceof TypeVariableSignature);
        assertEquals("Foo", ((TypeVariableSignature) sig).getIdentifier());
        assertSame(sig, parse(ctxt, "TFoo;"));
    }

    @Test
    public void testArrayOfSimpleType() {
        ClassContext ctxt = new TestClassContext();
        final TypeSignature sig = parse(ctxt, "[B");
        assertTrue(sig instanceof ArrayTypeSignature);
        assertEquals(BaseTypeSignature.B, ((ArrayTypeSignature) sig).getElementTypeSignature());
        assertSame(sig, parse(ctxt, "[B"));
    }

    @Test
    public void testArrayOfArrayOfSimpleType() {
        ClassContext ctxt = new TestClassContext();
        final TypeSignature sig = parse(ctxt, "[[J");
        assertTrue(sig instanceof ArrayTypeSignature);
        assertTrue(((ArrayTypeSignature) sig).getElementTypeSignature() instanceof ArrayTypeSignature);
        assertEquals(BaseTypeSignature.J, ((ArrayTypeSignature) ((ArrayTypeSignature) sig).getElementTypeSignature()).getElementTypeSignature());
        assertSame(sig, parse(ctxt, "[[J"));
    }

    @Test
    public void testReallyComplex() {
        ClassContext ctxt = new TestClassContext();
        final TypeSignature sig = parse(ctxt, "Lfoo/bar/baz/Zap<**+[TT;-TT;Lblah/Bzzt;*>.Zap<-TXxxxxx;>;;");
        assertTrue(sig instanceof ClassTypeSignature);
        assertTrue(sig instanceof NestedClassTypeSignature);
        final ClassTypeSignature outer = ((NestedClassTypeSignature) sig).getEnclosing();
        assertEquals(6, outer.getTypeArguments().size());
        assertSame(outer.getTypeArguments().get(0), AnyTypeArgument.INSTANCE);
        assertSame(outer.getTypeArguments().get(1), AnyTypeArgument.INSTANCE);
        final TypeArgument arg2 = outer.getTypeArguments().get(2);
        assertTrue(arg2 instanceof BoundTypeArgument);
        assertEquals(Variance.COVARIANT, ((BoundTypeArgument) arg2).getVariance());
        final ReferenceTypeSignature bta2Type = ((BoundTypeArgument) arg2).getBound();
        assertTrue(bta2Type instanceof ArrayTypeSignature);
        final TypeSignature bta2TypeType = ((ArrayTypeSignature) bta2Type).getElementTypeSignature();
        assertTrue(bta2TypeType instanceof TypeVariableSignature);
        assertEquals("T", ((TypeVariableSignature) bta2TypeType).getIdentifier());
        final TypeArgument arg3 = outer.getTypeArguments().get(3);
        assertTrue(arg3 instanceof BoundTypeArgument);
        assertEquals(Variance.CONTRAVARIANT, ((BoundTypeArgument) arg3).getVariance());
        final ReferenceTypeSignature bta3Type = ((BoundTypeArgument) arg3).getBound();
        assertTrue(bta3Type instanceof TypeVariableSignature);
        assertSame(bta2TypeType, bta3Type);
        final TypeArgument arg4 = outer.getTypeArguments().get(4);
        assertTrue(arg4 instanceof BoundTypeArgument);
        assertEquals(Variance.INVARIANT, ((BoundTypeArgument) arg4).getVariance());
        assertTrue(((BoundTypeArgument) arg4).getBound() instanceof ClassTypeSignature);
        assertEquals("Bzzt", ((ClassTypeSignature) ((BoundTypeArgument) arg4).getBound()).getIdentifier());
        assertSame(outer.getTypeArguments().get(5), AnyTypeArgument.INSTANCE);
        assertSame(outer.getIdentifier(), ((NestedClassTypeSignature) sig).getIdentifier());
        assertEquals(1, ((NestedClassTypeSignature) sig).getTypeArguments().size());
    }
}
