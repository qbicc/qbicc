package org.qbicc.plugin.instanceofcheckcast;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * A BasicBlockBuilder which replaces instanceof/checkcast operations with
 * either inline code sequences that implement common cases or a call to
 * the out-of-line VMHelper routine that provides a full implementation.
 */
public class InstanceOfCheckCastBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public InstanceOfCheckCastBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value checkcast(Value input, Value toType, Value toDimensions, CheckCast.CastType kind, ReferenceType type) {
        // First, see if we can statically prove the cast must always succeed
        if (toType instanceof TypeLiteral && toDimensions instanceof IntegerLiteral) {
            // We know the exact toType at compile time
            ObjectType toTypeOT = (ObjectType) ((TypeLiteral) toType).getValue(); // by construction in MemberResolvingBasicBlockBuilder.checkcast
            int dims = ((IntegerLiteral) toDimensions).intValue();
            if (isAlwaysAssignable(input.getType(), toTypeOT, dims)) {
                return input;
            }
        } else if (kind.equals(CheckCast.CastType.ArrayStore) && isEffectivelyFinal(type.getUpperBound())) {
            // We do not have toType as a compile-time literal, but since the element type of the array we are
            // storing into is effectivelyFinal we do not need to worry about co-variant array subtyping.
            if (input.getType() instanceof ReferenceType && ((ReferenceType) input.getType()).instanceOf(type.getUpperBound())) {
                return input;
            }
        }

        // Second, null can be trivially cast to any reference type
        if (input instanceof NullLiteral) {
            return bitCast(input, type);
        }

        // If we get here, we have to generate code for a test of some form.

        // Null can be cast to any reference type and the test is cheap; if null branch to "pass"
        final BlockLabel pass = new BlockLabel();
        final BlockLabel fail = new BlockLabel();
        final BlockLabel dynCheck = new BlockLabel();
        if_(isEq(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType())), pass, dynCheck);

        // raise exception on failure.
        try {
            begin(fail);
            MethodElement thrower = ctxt.getVMHelperMethod(kind.equals(CheckCast.CastType.Cast) ? "raiseClassCastException" : "raiseArrayStoreException");
            getFirstBuilder().callNoReturn(getFirstBuilder().staticMethod(thrower), List.of());
        } catch (BlockEarlyTermination ignored) {
            // continue
        }

        try {
            // Generate the code for a check; ideally inline but if that isn't possible then out-of-line.
            begin(dynCheck);
            boolean inlinedTest;
            if (toType instanceof TypeLiteral && toDimensions instanceof IntegerLiteral) {
                // We know the exact toType at compile time
                ObjectType toTypeOT = (ObjectType) ((TypeLiteral) toType).getValue(); // by construction in MemberResolvingBasicBlockBuilder.checkcast
                int dims = ((IntegerLiteral) toDimensions).intValue();
                inlinedTest = generateTypeTest(input, toTypeOT, dims, pass, fail);
            } else {
                inlinedTest = generateTypeTest(input, toType, toDimensions, type, pass, fail);
            }
            if (!inlinedTest) {
                String helperName;
                if (kind.equals(CheckCast.CastType.Cast)) {
                    helperName = toType instanceof TypeLiteral ? "checkcast_typeId" : "checkcast_class";
                } else {
                    helperName = "arrayStoreCheck";
                }
                getFirstBuilder().call(getFirstBuilder().staticMethod(ctxt.getVMHelperMethod(helperName)), List.of(input, toType, toDimensions));
                goto_(pass);
            }
        } catch (BlockEarlyTermination ignored) {
            // continue
        }

        begin(pass);
        return bitCast(input, type);
    }

    public Value instanceOf(final Value input, final ObjectType expectedType, int expectedDimensions) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        // "null" instanceof <X> is always false
        if (input instanceof NullLiteral) {
            return ctxt.getLiteralFactory().literalOf(false);
        }

        // statically true instanceof checks reduce to input != null
        ValueType actualType = input.getType();
        if (actualType instanceof ReferenceType) {
            if (expectedDimensions == 0 && ((ReferenceType) actualType).instanceOf(expectedType)) {
                // the input is known to be an instance
                return super.isNe(input, lf.zeroInitializerLiteralOfType(actualType));
            }
        }

        // If the expectedType isn't live in the RTAInfo, then we know we can never have an
        // instanceof it at runtime. Transform all such instanceofs to false
        RTAInfo info = RTAInfo.get(ctxt);
        if (expectedType instanceof ClassObjectType || expectedType instanceof InterfaceObjectType) {
            LoadedTypeDefinition vtd = expectedType.getDefinition().load();
            if (vtd.isInterface()) {
                if (!info.isReachableInterface(vtd)) {
                    return ctxt.getLiteralFactory().literalOf(false);
                }
            } else {
                if (!info.isReachableClass(vtd)) {
                    return ctxt.getLiteralFactory().literalOf(false);
                }
            }
        }

        // If we get here, we have to generate a real dynamic typecheck.
        final BlockLabel fail = new BlockLabel();
        final BlockLabel notNull = new BlockLabel();
        final BlockLabel allDone = new BlockLabel();

        // first, handle null
        if_(isEq(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType())), fail, notNull);

        Value passResult = null;
        BlockLabel passLabel = null;
        try {
            begin(notNull);
            // invariant: passInline label is only used by generateTypeTest when it returns true;
            final BlockLabel passInline = new BlockLabel();
            boolean inlinedTest = generateTypeTest(input, expectedType, expectedDimensions, passInline, fail);
            if (!inlinedTest) {
                MethodElement helper = ctxt.getVMHelperMethod("instanceof_typeId");
                passResult = getFirstBuilder().call(getFirstBuilder().staticMethod(helper),
                    List.of(input, lf.literalOfType(expectedType), lf.literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), expectedDimensions)));
                passLabel = notNull;
                goto_(allDone);
            } else {
                begin(passInline);
                goto_(allDone);
                passLabel = passInline;
                passResult = lf.literalOf(true);
            }
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        begin(fail);
        goto_(allDone);

        begin(allDone);
        PhiValue phi = phi(ctxt.getTypeSystem().getBooleanType(), allDone);
        phi.setValueForBlock(ctxt, getCurrentElement(), fail, lf.literalOf(false));
        if (passLabel != null) {
            phi.setValueForBlock(ctxt, getCurrentElement(), passLabel, passResult);
        }
        return phi;
    }

    public Value classOf(final Value typeId) {
        MethodElement methodElement = ctxt.getVMHelperMethod("classof_from_typeid");
        if (typeId instanceof TypeLiteral) {
            ValueType valueType = ((TypeLiteral) typeId).getValue();
            if (valueType instanceof ReferenceType) {
                ctxt.error(getLocation(), "Class of reference type");
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(methodElement.getType().getReturnType());
            }
        }

        ctxt.warning(getLocation(), "Lowering classOf to incomplete VMHelper stub");
        List<Value> args = List.of(typeId);
        return notNull(getFirstBuilder().call(getFirstBuilder().staticMethod(methodElement), args));
    }

    // Used when we know the exact type we are testing for at compile time (checkcast and instanceof bytecodes)
    private boolean generateTypeTest(Value input, ObjectType toType, int toDimensions, BlockLabel pass, BlockLabel fail) {
        if (toDimensions != 0) return false; // For now, no inline sequence for reference arrays.

        LiteralFactory lf = ctxt.getLiteralFactory();

        if (toType instanceof PrimitiveArrayObjectType) {
            DefinedTypeDefinition dtd = Layout.get(ctxt).getArrayContentField(toType).getEnclosingType();
            LoadedTypeDefinition arrayVTD = dtd.load();
            final int primArrayTypeId = arrayVTD.getTypeId();
            Value inputTypeId = typeIdOf(referenceHandle(input));
            if_(isEq(inputTypeId, lf.literalOf(primArrayTypeId)), pass, fail);
        } else if (toType instanceof InterfaceObjectType) {
            // 2 - expectedType statically known to be an interface
            SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
            LoadedTypeDefinition vtdExpectedType = toType.getDefinition().load();
            final int byteIndex = tables.getInterfaceByteIndex(vtdExpectedType);
            final int mask = tables.getInterfaceBitMask(vtdExpectedType);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(getDelegate().getCurrentElement());
            Value inputTypeId = typeIdOf(referenceHandle(input));
            // typeIdStruct = qbicc_typeid_array[typeId]
            ValueHandle typeIdStruct = elementOf(globalVariable(typeIdGlobal), inputTypeId);
            // bits = &typeIdStruct.interfaceBits
            ValueHandle bits = memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            // thisByte = bits[byteIndex]
            Value thisByte = load(elementOf(bits, lf.literalOf(byteIndex)), MemoryAtomicityMode.UNORDERED);
            // maskedValue = thisByte & mask
            Value maskedValue = and(thisByte, lf.literalOf(mask));
            if_(isEq(maskedValue, lf.literalOf(mask)), pass, fail);
        } else {
            // 3 - expectedType statically known to be a class
            // There are two sub cases when dealing with classes:
            // A - leaf classes that have no subclasses can be a direct compare
            // B - non-leaf classes need a subtract + compare
            ClassObjectType cotExpectedType = (ClassObjectType) toType;
            LoadedTypeDefinition vtdExpectedType = cotExpectedType.getDefinition().load();
            Value inputTypeId = typeIdOf(referenceHandle(input));
            final int typeId = vtdExpectedType.getTypeId();
            final int maxSubId = vtdExpectedType.getMaximumSubtypeId();
            Literal vtdTypeId = lf.literalOf(typeId);
            if (typeId == maxSubId) {
                // "leaf" class case - use direct comparison
                if_(isEq(inputTypeId, vtdTypeId), pass, fail);
            } else {
                // "non-leaf" class case
                // (instanceId - castClassId <= (castClassId.range - castClassId)
                IntegerLiteral allowedRange = lf.literalOf(maxSubId - typeId);
                Value subtract = sub(inputTypeId, vtdTypeId);
                if_(isLe(subtract, allowedRange), pass, fail);
            }
        }

        return true;
    }


    // Used when we don't know the exact type we are testing for at compile time (array store check, Class.cast).
    // However, we may still know enough about the allowable kinds of types to generate an inlined test.
    private boolean generateTypeTest(Value input, Value toType, Value toDimensions, ReferenceType shape, BlockLabel pass, BlockLabel fail) {
        // A Class that isn't java.lang.Object (and therefore excludes Arrays); generate subclass test
        if (shape.getUpperBound() instanceof ClassObjectType && shape.getUpperBound().hasSuperClass()) {
            SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
            // 1. use toType (a TypeId) to load the corresponding maxTypeId
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(getDelegate().getCurrentElement());
            ValueHandle typeIdStruct = elementOf(globalVariable(typeIdGlobal), toType);
            Value maxTypeId = load(memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("maxSubTypeId")), MemoryAtomicityMode.UNORDERED);

            // 2. Test passes iff toType <= valueTypeId <= maxTypeId
            //    Note: we could instead test: (valueTypeId - toType) <=_unsigned (maxTypeId - toType) since typeIds are all positive
            Value valueTypeId = typeIdOf(referenceHandle(input));
            if_(and(isLe(toType, valueTypeId), isLe(valueTypeId, maxTypeId)), pass, fail);
            return true;
        }

        return false;
    }

    private boolean isAlwaysAssignable(ValueType inputValueType, ObjectType toType, int toDimensions) {
        if (!(inputValueType instanceof ReferenceType)) {
            return false;
        }
        ReferenceType inputType = (ReferenceType) inputValueType;
        if (toDimensions == 0) {
            return inputType.instanceOf(toType);
        } else if (inputType.getUpperBound() instanceof ReferenceArrayObjectType) {
            ReferenceArrayObjectType inputArrayType = ((ReferenceArrayObjectType) inputType.getUpperBound());
            if (inputArrayType.getDimensionCount() == toDimensions) {
                return inputType.instanceOf(inputArrayType.getElementType());
            } else if (inputArrayType.getDimensionCount() > toDimensions) {
                return !toType.hasSuperClass(); // only alwaysAssignable if toType is java.lang.Object
            }
        }

        return false;
    }

    private boolean isEffectivelyFinal(ObjectType type) {
        if (type instanceof PrimitiveArrayObjectType) {
            return true;
        }
        if (type instanceof ReferenceArrayObjectType || type instanceof InterfaceObjectType) {
            return false;
        }
        // A class is effectively final if it has no live subclasses.
        LoadedTypeDefinition vtd = type.getDefinition().load();
        final int typeId = vtd.getTypeId();
        final int maxSubId = vtd.getMaximumSubtypeId();
        return typeId == maxSubId;
    }
}
