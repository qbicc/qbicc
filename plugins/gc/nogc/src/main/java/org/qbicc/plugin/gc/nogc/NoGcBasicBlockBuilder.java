package org.qbicc.plugin.gc.nogc;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public class NoGcBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NoGcBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value new_(final ClassObjectType type) {
        NoGc noGc = NoGc.get(ctxt);
        Layout layout = Layout.get(ctxt);
        Layout.LayoutInfo info = layout.getInstanceLayoutInfo(type.getDefinition());
        CompoundType compoundType = info.getCompoundType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerLiteral align = lf.literalOf(compoundType.getAlign());
        Value ptrVal;
        if (type.isSubtypeOf(noGc.getStackObjectType()) /*|| objectDoesNotEscape && objectIsSmallEnough */) {
            ptrVal = stackAllocate(compoundType, lf.literalOf(1), align);
        } else {
            long size = compoundType.getSize();
            ptrVal = notNull(call(staticMethod(noGc.getAllocateMethod()), List.of(lf.literalOf(size), align)));
        }
        Value oop = valueConvert(ptrVal, type.getReference());
        ValueHandle oopHandle = referenceHandle(oop);

        // zero initialize the object's instance fields (but not the header fields that are defined in java.lang.Object)
        LoadedTypeDefinition curClass = type.getDefinition().load();
        while (curClass.hasSuperClass()) {
            curClass.eachField(f -> {
                if (!f.isStatic()) {
                    store(instanceFieldOf(oopHandle, f), lf.zeroInitializerLiteralOfType(f.getType()), MemoryAtomicityMode.NONE);
                }
            });
            curClass = curClass.getSuperClass();
        }

        // now initialize the object header (aka fields of java.lang.Object)
        initializeObjectHeader(oopHandle, layout, type.getDefinition().load().getType());

        fence(MemoryAtomicityMode.RELEASE);
        return oop;
    }

    public Value newArray(final ArrayObjectType arrayType, Value size) {
        NoGc noGc = NoGc.get(ctxt);
        Layout layout = Layout.get(ctxt);
        FieldElement arrayContentField = layout.getArrayContentField(arrayType);
        Layout.LayoutInfo info = layout.getInstanceLayoutInfo(arrayContentField.getEnclosingType());
        CompoundType compoundType = info.getCompoundType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerLiteral align = lf.literalOf(compoundType.getAlign());
        IntegerLiteral baseSize = lf.literalOf(compoundType.getSize());
        IntegerType sizeType = (IntegerType) size.getType();
        if (sizeType.getMinBits() < 64) {
            size = extend(size, ctxt.getTypeSystem().getSignedInteger64Type());
        }
        Value realSize = add(baseSize, multiply(lf.literalOf(arrayType.getElementType().getSize()), size));
        Value ptrVal = notNull(call(staticMethod(noGc.getAllocateMethod()), List.of(realSize, align)));

        call(staticMethod(noGc.getZeroMethod()), List.of(ptrVal, realSize));
        Value arrayPtr = valueConvert(ptrVal, arrayType.getReference());
        ValueHandle arrayHandle = referenceHandle(arrayPtr);

        initializeObjectHeader(arrayHandle, layout, arrayContentField.getEnclosingType().load().getType());

        store(instanceFieldOf(arrayHandle, layout.getArrayLengthField()), truncate(size, ctxt.getTypeSystem().getSignedInteger32Type()), MemoryAtomicityMode.NONE);
        if (arrayType instanceof ReferenceArrayObjectType) {
            ReferenceArrayObjectType refArrayType = (ReferenceArrayObjectType)arrayType;
            store(instanceFieldOf(arrayHandle, layout.getRefArrayDimensionsField()), lf.literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), refArrayType.getDimensionCount()), MemoryAtomicityMode.NONE);
            store(instanceFieldOf(arrayHandle, layout.getRefArrayElementTypeIdField()), lf.literalOfType(refArrayType.getLeafElementType()), MemoryAtomicityMode.NONE);
        }

        fence(MemoryAtomicityMode.RELEASE);
        return arrayPtr;
    }

    public Value clone(final Value object) {
        ValueType objType = object.getType();
        NoGc noGc = NoGc.get(ctxt);
        Layout layout = Layout.get(ctxt);
        if (objType instanceof ClassObjectType) {
            ClassObjectType type = (ClassObjectType) objType;
            Layout.LayoutInfo info = layout.getInstanceLayoutInfo(type.getDefinition());
            LiteralFactory lf = ctxt.getLiteralFactory();
            CompoundType compoundType = info.getCompoundType();
            IntegerLiteral size = lf.literalOf(compoundType.getSize());
            IntegerLiteral align = lf.literalOf(compoundType.getAlign());
            Value ptrVal;
            if (type.isSubtypeOf(noGc.getStackObjectType())) {
                ptrVal = stackAllocate(compoundType, lf.literalOf(1), align);
            } else {
                ptrVal = notNull(call(staticMethod(noGc.getAllocateMethod()), List.of(size, align)));
            }
            // TODO: replace with field-by-field copy once we have a redundant assignment elimination optimization
            // TODO: if/when we put a thinlock, default hashcode, or GC state bits in the object header we need to properly initialize them.
            call(staticMethod(noGc.getCopyMethod()), List.of(ptrVal, valueConvert(object, (WordType) ptrVal.getType()), size));
            fence(MemoryAtomicityMode.RELEASE);
            return valueConvert(ptrVal, type.getReference());
        } else if (objType instanceof ArrayObjectType) {
            ctxt.error(getLocation(), "Array allocations not supported until layout supports arrays");
            throw new BlockEarlyTermination(unreachable());
        } else {
            return super.clone(object);
        }
    }

    // Currently there is only one header field, but abstract into a helper method so we only have one place to update later!
    private void initializeObjectHeader(ValueHandle oopHandle, Layout layout, ObjectType objType) {
        FieldElement typeId = layout.getObjectTypeIdField();
        store(instanceFieldOf(oopHandle, typeId),  ctxt.getLiteralFactory().literalOfType(objType), MemoryAtomicityMode.NONE);
    }
}
