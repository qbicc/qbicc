package cc.quarkus.qcc.plugin.gc.nogc;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockEarlyTermination;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.FieldElement;

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
            ptrVal = invokeValueStatic(noGc.getAllocateMethod(), List.of(lf.literalOf(size), align));
        }
        Value oop = valueConvert(ptrVal, type.getReference());
        ValueHandle oopHandle = referenceHandle(oop);

        // zero initialize the object's instance fields (but not the header fields that are defined in java.lang.Object)
        ValidatedTypeDefinition curClass = type.getDefinition().validate();
        while (curClass.hasSuperClass()) {
            curClass.eachField(f -> {
                if (!f.isStatic()) {
                    store(instanceFieldOf(oopHandle, f), lf.zeroInitializerLiteralOfType(f.getType(List.of())), MemoryAtomicityMode.UNORDERED);
                }
            });
            curClass = curClass.getSuperClass();
        }

        // now initialize the object header (which is defined via instance fields of java.lang.Object)
        FieldElement typeId = layout.getObjectTypeIdField();
        curClass.eachField(f -> {
            if (!f.isStatic()) {
                if (f.equals(typeId)) {
                    store(instanceFieldOf(oopHandle, typeId), lf.literalOfType(type.getDefinition().validate().getType()), MemoryAtomicityMode.UNORDERED);
                } else {
                    // Currently there aren't any fields besides typeId, but protect ourselves from hard to find bugs later...
                    store(instanceFieldOf(oopHandle, f), lf.zeroInitializerLiteralOfType(f.getType(List.of())), MemoryAtomicityMode.UNORDERED);
                }
            }
            });

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
        Value ptrVal = invokeValueStatic(noGc.getAllocateMethod(), List.of(realSize, align));
        // todo: zero-fill...
        // todo: initialize object header
        return valueConvert(ptrVal, arrayType.getReference());
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
                ptrVal = invokeValueStatic(noGc.getAllocateMethod(), List.of(size, align));
            }
            // TODO: replace with field-by-field copy once we have a redundant assignment elimination optimization
            // TODO: if/when we put a thinlock, default hashcode, or GC state bits in the object header we need to properly initialize them.
            invokeStatic(noGc.getCopyMethod(), List.of(ptrVal, valueConvert(object, (WordType) ptrVal.getType()), size));
            return valueConvert(ptrVal, type.getReference());
        } else if (objType instanceof ArrayObjectType) {
            ctxt.error(getLocation(), "Array allocations not supported until layout supports arrays");
            throw new BlockEarlyTermination(unreachable());
        } else {
            return super.clone(object);
        }
    }
}
