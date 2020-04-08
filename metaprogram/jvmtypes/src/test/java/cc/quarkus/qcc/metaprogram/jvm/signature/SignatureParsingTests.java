package cc.quarkus.qcc.metaprogram.jvm.signature;

import static org.junit.jupiter.api.Assertions.*;

import cc.quarkus.qcc.context.Context;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class SignatureParsingTests {

    static final String JLO_SIG_STR = "L" + "java/lang/Object;";

    @Test
    public void testBasicTypes() {
        final Context ctxt = new Context(false);
        ctxt.run(() -> {
            assertEquals(TypeSignature.parseTypeSignature("B"), BaseTypeSignature.BYTE);
            assertEquals(TypeSignature.parseTypeSignature("S"), BaseTypeSignature.SHORT);
            assertEquals(TypeSignature.parseTypeSignature("I"), BaseTypeSignature.INT);
            assertEquals(TypeSignature.parseTypeSignature("J"), BaseTypeSignature.LONG);
            assertEquals(TypeSignature.parseTypeSignature("C"), BaseTypeSignature.CHAR);
            assertEquals(TypeSignature.parseTypeSignature("Z"), BaseTypeSignature.BOOLEAN);
            assertEquals(TypeSignature.parseTypeSignature("F"), BaseTypeSignature.FLOAT);
            assertEquals(TypeSignature.parseTypeSignature("D"), BaseTypeSignature.DOUBLE);
            assertEquals(TypeSignature.parseTypeSignature("V"), BaseTypeSignature.VOID);
        });
    }

    @Test
    public void testReferenceTypes() {
        final Context ctxt = new Context(false);
        ctxt.run(() -> {
            final TypeSignature sig = TypeSignature.parseTypeSignature(JLO_SIG_STR);
            assertTrue(sig instanceof ClassTypeSignature);
            final ClassTypeSignature cts = (ClassTypeSignature) sig;
            assertEquals("Object", cts.getSimpleName());
            assertEquals("lang", cts.getPackageName().getSimpleName());
            assertEquals("java", cts.getPackageName().getEnclosing().getSimpleName());
            assertSame(sig, TypeSignature.parseTypeSignature(JLO_SIG_STR));
        });
    }

    @Test
    public void testSimpleTypeVariable() {
        final Context ctxt = new Context(false);
        ctxt.run(() -> {
            final TypeSignature sig = TypeSignature.parseTypeSignature("TFoo;");
            assertTrue(sig instanceof TypeVariableSignature);
            final TypeVariableSignature tvs = (TypeVariableSignature) sig;
            assertEquals("Foo", tvs.getSimpleName());
            assertSame(tvs, TypeSignature.parseTypeSignature("TFoo;"));
        });
    }

    @Test
    public void testArrayOfSimpleType() {
        final Context ctxt = new Context(false);
        ctxt.run(() -> {
            final TypeSignature sig = TypeSignature.parseTypeSignature("[B");
            assertTrue(sig instanceof ArrayTypeSignature);
            final ArrayTypeSignature ats = (ArrayTypeSignature) sig;
            assertEquals(BaseTypeSignature.BYTE, ats.getMemberSignature());
            assertSame(ats, TypeSignature.parseTypeSignature("[B"));
        });
    }

    @Test
    public void testArrayOfArrayOfSimpleType() {
        final Context ctxt = new Context(false);
        ctxt.run(() -> {
            final TypeSignature sig = TypeSignature.parseTypeSignature("[[J");
            assertTrue(sig instanceof ArrayTypeSignature);
            final ArrayTypeSignature ats = (ArrayTypeSignature) sig;
            assertTrue(ats.getMemberSignature() instanceof ArrayTypeSignature);
            final ArrayTypeSignature ats2 = (ArrayTypeSignature) ats.getMemberSignature();
            assertEquals(BaseTypeSignature.LONG, ats2.getMemberSignature());
            assertSame(sig, TypeSignature.parseTypeSignature("[[J"));
        });
    }

    @Test
    public void testReallyComplex() {
        final Context ctxt = new Context(false);
        ctxt.run(() -> {
            final TypeSignature sig = TypeSignature.parseTypeSignature("Lfoo/bar/baz/Zap<**+[TT;-TT;Lblah/Bzzt;*>.Zap<-TXxxxxx;>;;");
            assertTrue(sig instanceof ClassTypeSignature);
            final ClassTypeSignature inner = (ClassTypeSignature) sig;
            assertTrue(inner.hasEnclosing());
            final ClassTypeSignature outer = inner.getEnclosing();
            assertEquals(6, outer.getTypeArgumentCount());
            assertSame(outer.getTypeArgument(0), ClassTypeSignature.AnyTypeArgument.INSTANCE);
            assertSame(outer.getTypeArgument(1), ClassTypeSignature.AnyTypeArgument.INSTANCE);
            final ClassTypeSignature.TypeArgument arg2 = outer.getTypeArgument(2);
            assertTrue(arg2 instanceof ClassTypeSignature.BoundTypeArgument);
            final ClassTypeSignature.BoundTypeArgument bta2 = (ClassTypeSignature.BoundTypeArgument) arg2;
            assertEquals(ClassTypeSignature.Variance.COVARIANT, bta2.getVariance());
            final ReferenceTypeSignature bta2Type = bta2.getValue();
            assertTrue(bta2Type instanceof ArrayTypeSignature);
            final TypeSignature bta2TypeType = ((ArrayTypeSignature) bta2Type).getMemberSignature();
            assertTrue(bta2TypeType instanceof TypeVariableSignature);
            assertEquals("T", ((TypeVariableSignature) bta2TypeType).getSimpleName());
            final ClassTypeSignature.TypeArgument arg3 = outer.getTypeArgument(3);
            assertTrue(arg3 instanceof ClassTypeSignature.BoundTypeArgument);
            final ClassTypeSignature.BoundTypeArgument bta3 = (ClassTypeSignature.BoundTypeArgument) arg3;
            assertEquals(ClassTypeSignature.Variance.CONTRAVARIANT, bta3.getVariance());
            final ReferenceTypeSignature bta3Type = bta3.getValue();
            assertTrue(bta3Type instanceof TypeVariableSignature);
            assertSame(bta2TypeType, bta3Type);
            final ClassTypeSignature.TypeArgument arg4 = outer.getTypeArgument(4);
            assertTrue(arg4 instanceof ClassTypeSignature.BoundTypeArgument);
            final ClassTypeSignature.BoundTypeArgument bta4 = (ClassTypeSignature.BoundTypeArgument) arg4;
            assertEquals(ClassTypeSignature.Variance.INVARIANT, bta4.getVariance());
            assertEquals("Bzzt", ((ClassTypeSignature)bta4.getValue()).getSimpleName());
            assertSame(outer.getTypeArgument(5), ClassTypeSignature.AnyTypeArgument.INSTANCE);
            assertSame(outer.getSimpleName(), inner.getSimpleName());
            assertEquals(1, inner.getTypeArgumentCount());
        });
    }
}
