package org.qbicc.plugin.gc.common;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.plugin.coreclasses.BasicHeaderInitializer;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.StructType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public class GcBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    private final CompilationContext ctxt;
    private final CoreClasses coreClasses;

    public GcBasicBlockBuilder(final FactoryContext fc, BasicBlockBuilder delegate) {
        super(delegate);
        CompilationContext ctxt = getContext();
        this.ctxt = ctxt;
        coreClasses = CoreClasses.get(ctxt);
    }

    @Override
    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        AbstractGc gc = AbstractGc.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();
        Value refVal = null;
        boolean stackAlloc = false;
        if (typeId instanceof TypeIdLiteral tl && tl.getValue() instanceof ClassObjectType cot) {
            // We can only even attempt stack allocation if the typeId is a literal (ie, known precisely at compile time).
            if (cot.isSubtypeOf(gc.getStackObjectType()) /*|| objectDoesNotEscape && objectIsSmallEnough */) {
                StructType structType = Layout.get(ctxt).getInstanceLayoutInfo(cot.getDefinition()).getStructType();
                refVal = encodeReference(stackAllocate(structType, lf.literalOf(1), align), type.getReference());
                // zero initialize the allocated storage
                MethodElement method = gc.getZeroMethod();
                call(lf.literalOf(method), List.of(refVal, size));
                stackAlloc = true;
            }
        }
        if (refVal == null) {
            MethodElement method = gc.getAllocateMethod();
            refVal = notNull(bitCast(call(lf.literalOf(method), List.of(size)), type.getReference()));
        }

        BasicHeaderInitializer.initializeObjectHeader(ctxt, this, decodeReference(refVal), typeId, stackAlloc);
        return refVal;
    }

    @Override
    public Value newArray(final PrimitiveArrayObjectType arrayType, Value size) {
        LoadedTypeDefinition ltd = coreClasses.getArrayContentField(arrayType).getEnclosingType().load();
        StructType structType = Layout.get(ctxt).getInstanceLayoutInfo(ltd).getStructType();
        Value allocatedRef = allocateArray(structType, size, arrayType.getElementType().getSize());
        Value oop = bitCast(allocatedRef, arrayType.getReference());
        BasicHeaderInitializer.initializeArrayHeader(ctxt, this, decodeReference(oop), ctxt.getLiteralFactory().literalOfType(ltd.getClassType()), size);
        return oop;
    }

    @Override
    public Value newReferenceArray(final ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, Value size) {
        Layout layout = Layout.get(ctxt);
        LayoutInfo info = layout.getInstanceLayoutInfo(coreClasses.getRefArrayContentField().getEnclosingType());
        StructType structType = info.getStructType();
        Value allocatedRef = allocateArray(structType, size, ctxt.getTypeSystem().getReferenceSize());
        Value oop = bitCast(allocatedRef, arrayType.getReference());
        BasicHeaderInitializer.initializeRefArrayHeader(ctxt, this, decodeReference(oop), elemTypeId, dimensions, size);
        return oop;
    }

    private Value allocateArray(StructType structType, Value size, long elementSize) {
        AbstractGc gc = AbstractGc.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerLiteral baseSize = lf.literalOf(structType.getSize());
        IntegerType sizeType = (IntegerType) size.getType();
        if (sizeType.getMinBits() < 64) {
            size = extend(size, ctxt.getTypeSystem().getSignedInteger64Type());
        }
        assert Long.bitCount(elementSize) == 1;
        int elementShift = Long.numberOfTrailingZeros(elementSize);
        Value realSize = add(baseSize, elementShift == 0 ? size : shl(size, lf.literalOf((IntegerType)size.getType(), elementShift)));

        // Allocate and zero-initialize the storage
        return notNull(call(lf.literalOf(gc.getAllocateMethod()), List.of(realSize)));
    }
}
