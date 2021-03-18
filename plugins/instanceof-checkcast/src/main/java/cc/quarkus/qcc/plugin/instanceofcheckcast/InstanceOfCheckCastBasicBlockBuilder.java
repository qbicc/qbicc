package cc.quarkus.qcc.plugin.instanceofcheckcast;

import java.util.List;

import cc.quarkus.qcc.graph.CheckCast;
import cc.quarkus.qcc.graph.literal.TypeLiteral;
import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.type.definition.element.GlobalVariableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.InterfaceObjectType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.PrimitiveArrayObjectType;
import cc.quarkus.qcc.type.ReferenceArrayObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A BasicBlockBuilder which replaces instanceof/checkcast operations with calls
 * to RuntimeHelper APIs.
 */
public class InstanceOfCheckCastBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.plugin.instanceofcheckcast");

    private final CompilationContext ctxt;

    static final boolean PLUGIN_DISABLED = true;

    public InstanceOfCheckCastBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value checkcast(Value input, Value narrowInput, CheckCast.CastType kind, ReferenceType toType) {
        if (! (input.getType() instanceof ReferenceType)) {
            return super.checkcast(input ,narrowInput, kind, toType);
        }
        ReferenceType inputType = (ReferenceType)input.getType();

        // First, see if we can prove statically that the cast will always succeed.
        if (kind.equals(CheckCast.CastType.Cast) && narrowInput instanceof TypeLiteral) {
            ValueType desiredType = narrowInput.getType().getTypeType().getUpperBound();
            if (desiredType instanceof ReferenceType && inputType.instanceOf((ReferenceType) desiredType)) {
                // Statically safe cast
                return input;
            }
        } else if (kind.equals(CheckCast.CastType.ArrayStore)) {
            // If narrowInput's leaf element type is effectively final, then the cast is
            // statically safe if inputType is an instanceOf narrowInput's element type.
            if (narrowInput.getType() instanceof ReferenceType && ((ReferenceType) narrowInput.getType()).getUpperBound() instanceof ReferenceArrayObjectType) {
                ReferenceArrayObjectType arrayType = ((ReferenceArrayObjectType) ((ReferenceType) narrowInput.getType()).getUpperBound());
                if (effectivelyFinal(arrayType.getLeafElementType()) && inputType.instanceOf(arrayType.getElementType())) {
                    // Statically safe array store
                    return input;
                }
            }
        }

        // If we get here, we have to generate code for a test of some form.

        // Null can be cast to any reference type and the test is cheap; if null branch to "pass"
        final BlockLabel pass = new BlockLabel();
        final BlockLabel dynCheck = new BlockLabel();
        if_(isEq(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType())), pass, dynCheck);
        begin(dynCheck);

        // Now expand into either an inlined or outlined dynamic check based on what we know at compile time.
        if (kind.equals(CheckCast.CastType.Cast)) {
            if (narrowInput instanceof TypeLiteral) {
                // TODO: inline some of the easy cases.  For now, just call the VMHelper that does the check.
                MethodElement helper = ctxt.getVMHelperMethod("checkcast_typeId");
                getFirstBuilder().invokeStatic(helper, List.of(input, narrowInput));
                goto_(pass);
                begin(pass);
                return bitCast(input, toType);
            } else {
                // Not a case we inline; call the VMHelper that does the check.
                MethodElement helper = ctxt.getVMHelperMethod("checkcast_class");
                getFirstBuilder().invokeStatic(helper, List.of(input, narrowInput));
                goto_(pass);
                begin(pass);
                return bitCast(input, toType);
            }
        } else {
            Assert.assertTrue(kind.equals(CheckCast.CastType.ArrayStore));

            if (narrowInput.getType() instanceof ReferenceType && ((ReferenceType)narrowInput.getType()).getUpperBound() instanceof ReferenceArrayObjectType) {
                SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
                Layout layout = Layout.get(ctxt);
                ObjectType elemType = ((ReferenceArrayObjectType) ((ReferenceType) narrowInput.getType()).getUpperBound()).getElementObjectType();
                // Peel of simple and common cases of array store check that we should do inline
                if (elemType instanceof ClassObjectType && elemType.hasSuperClass()) {
                    // array is statically a Class[] and is not j.l.Object[]
                    // Therefore it cannot hold a runtime value that is an Interface[] or prim[][]

                    // 1. use the array's element typeId to load the element's maxTypeId
                    GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(getDelegate().getCurrentElement());
                    Value elemTypeId = load(instanceFieldOf(referenceHandle(narrowInput), layout.getRefArrayElementTypeIdField()), MemoryAtomicityMode.UNORDERED);
                    ValueHandle typeIdStruct = elementOf(globalVariable(typeIdGlobal), elemTypeId);
                    Value elemMaxTypeId = load(memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("maxSubTypeId")), MemoryAtomicityMode.UNORDERED);

                    // 2. get the typeId of the storedValue
                    Value valueTypeId = typeIdOf(referenceHandle(input));

                    // 3. Cast is legal iff elemTypeId <= valueTypeId <= elemMaxTypeId
                    BlockLabel fail = new BlockLabel();
                    if_(and(isLe(elemTypeId, valueTypeId), isLe(valueTypeId, elemMaxTypeId)), pass, fail);

                    // raise exception on failure.
                    begin(fail);
                    MethodElement helper = ctxt.getVMHelperMethod("raiseArrayStoreException");
                    getFirstBuilder().invokeStatic(helper, List.of());
                    unreachable();

                    // success block
                    begin(pass);
                    return bitCast(input, toType);
                }
            }

            // Not a case we inline; call the VMHelper that does the check.
            MethodElement helper = ctxt.getVMHelperMethod("arrayStoreCheck");
            getFirstBuilder().invokeStatic(helper, List.of(input, narrowInput));
            goto_(pass);
            begin(pass);
            return bitCast(input, toType);
        }
    }

    public Value instanceOf(final Value input, final ObjectType expectedType) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        // "null" instanceof <X> is always false
        if (input instanceof ZeroInitializerLiteral) {
            return ctxt.getLiteralFactory().literalOf(false);
        }

        // statically true instanceof checks are equal to x != null
        ValueType actualType = input.getType();
        if (actualType instanceof ReferenceType) {
            if (((ReferenceType) actualType).instanceOf(expectedType)) {
                // the input is known to be an instance
                return super.isNe(input, lf.zeroInitializerLiteralOfType(actualType));
            }
        }

        // If the class isn't loaded in the RTAInfo, then we know we can never have an
        // instanceof it at runtime. Transform all such instanceofs to false
        RTAInfo info = RTAInfo.get(ctxt);
        if (expectedType instanceof ClassObjectType || expectedType instanceof InterfaceObjectType) {
            ValidatedTypeDefinition vtd = expectedType.getDefinition().validate();
            if (vtd.isInterface()) {
                if (!info.isLiveInterface(vtd)) {
                    return ctxt.getLiteralFactory().literalOf(false);
                }
            } else {
                if (!info.isLiveClass(vtd)) {
                    return ctxt.getLiteralFactory().literalOf(false);
                }
            }
        } else if (expectedType instanceof ReferenceArrayObjectType) {
            // TODO: check element type is in RTAInfo.  Can't do that till some other PRs land
        }

        /* Set up the runtime checks here for the 3 major cases:
         * 1 - expectedType statically known to be an array class
         * 2 - expectedType statically known to be an interface
         * 3 - expectedType statically known to be a class
         * The check takes the form of:
         *   boolean result = false;
         *   if (input != null) result = doCheck(input, expectedType);
         *   return result;
         */
        final BlockLabel notNullLabel = new BlockLabel();
        final BlockLabel afterCheckLabel = new BlockLabel();
        final ZeroInitializerLiteral nullLiteral = lf.zeroInitializerLiteralOfType(expectedType);

        BasicBlock incomingBlock = if_(isNe(input, nullLiteral), notNullLabel, afterCheckLabel);
        begin(notNullLabel);
        Value result = null;
        if (expectedType instanceof ArrayObjectType) {
            // 1 - expectedType statically known to be an array class
            if (expectedType instanceof PrimitiveArrayObjectType) {
                DefinedTypeDefinition dtd = Layout.get(ctxt).getArrayContentField(expectedType).getEnclosingType();
                ValidatedTypeDefinition arrayVTD = dtd.validate();
                final int primArrayTypeId = arrayVTD.getTypeId();
                Value inputTypeId = typeIdOf(referenceHandle(input));
                result = super.isEq(inputTypeId, lf.literalOf(primArrayTypeId));
            } else {
                // Layout#getRefArrayDimensionsField
                Layout layout = Layout.get(ctxt);
                ValidatedTypeDefinition arrayVTD = layout.getArrayValidatedTypeDefinition("[ref");
                final int refArrayTypeId = arrayVTD.getTypeId();

                //Value inputTypeId = typeIdOf(referenceHandle(input));
                //Value isRefArray = super.isEq(inputTypeId, lf.literalOf(refArrayTypeId));
                //Value inputDims = load(instanceFieldOf(referenceHandle(input), layout.getRefArrayDimensionsField()), MemoryAtomicityMode.UNORDERED);
                //TODO - get the dims from the ReferenceArrayObjectType - waiting on other PRs
                /* 
                if (inputTypeId == refTypeId) {
                    if (inputDims == expectedType.dims) {
                        if (inputElementTypeId == expectedType.typeId) {
                            result = true
                        } else {
                            subtype check or interface type check
                        }
                    } else if (inputDims > expectedType.dims) {
                        // need to check
                        object or interface implemented by arrays
                    } else if (inputDims < expectedType.dims) {
                        result = false;
                    }
                } else {
                    result = false
                }
                */
                result = lf.literalOf(false);
            }
        } else if (expectedType instanceof InterfaceObjectType) {
            // 2 - expectedType statically known to be an interface
            SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
            ValidatedTypeDefinition vtdExpectedType = expectedType.getDefinition().validate();
            final int byteIndex = tables.getInterfaceByteIndex(vtdExpectedType);
            final int mask = tables.getInterfaceBitMask(vtdExpectedType);
            GlobalVariableElement typeIdGlobal = tables.getAndRegisterGlobalTypeIdArray(getDelegate().getCurrentElement());
            Value inputTypeId = typeIdOf(referenceHandle(input));
            // typeIdStruct = qcc_typeid_array[typeId]
            ValueHandle typeIdStruct = elementOf(globalVariable(typeIdGlobal), inputTypeId);
            // bits = &typeIdStruct.interfaceBits
            ValueHandle bits = memberOf(typeIdStruct, tables.getGlobalTypeIdStructType().getMember("interfaceBits"));
            // thisByte = bits[byteIndex] 
            Value thisByte = load(elementOf(bits, lf.literalOf(byteIndex)), MemoryAtomicityMode.UNORDERED);
            // maskedValue = thisByte & mask
            Value maskedValue = and(thisByte, lf.literalOf(mask));
            
            // TODO: this can probably be replaced with `maskedValue != 0`
            result = super.isEq(maskedValue, lf.literalOf(mask));
        } else {
            // 3 - expectedType statically known to be a class
            // There are two sub cases when dealing with classes:
            // A - leaf classes that have no subclasses can be a direct compare
            // B - non-leaf classes need a subtract + compare
            ClassObjectType cotExpectedType = (ClassObjectType)expectedType;
            ValidatedTypeDefinition vtdExpectedType = cotExpectedType.getDefinition().validate();
            Value inputTypeId = typeIdOf(referenceHandle(input));
            final int typeId = vtdExpectedType.getTypeId();
            final int maxSubId = vtdExpectedType.getMaximumSubtypeId();
            Literal vtdTypeId = lf.literalOf(typeId);
            if (typeId == maxSubId) {
                // "leaf" class case - use direct comparison
                result = super.isEq(inputTypeId, vtdTypeId);
            } else {
                // "non-leaf" class case
                // (instanceId - castClassId <= (castClassId.range - castClassId)
                IntegerLiteral allowedRange = lf.literalOf(maxSubId - typeId);
                Value subtract = sub(inputTypeId, vtdTypeId);
                result = super.isLe(subtract, allowedRange);
            }
        }
        goto_(afterCheckLabel);
        begin(afterCheckLabel);

        PhiValue phi = phi(ctxt.getTypeSystem().getBooleanType(), afterCheckLabel);
        phi.setValueForBlock(ctxt, getCurrentElement(), incomingBlock, lf.literalOf(false));
        phi.setValueForBlock(ctxt, getCurrentElement(), notNullLabel, result);
        return phi;

    }

    Value generateCallToRuntimeHelper(final Value input, ObjectType expectedType) {
        // This code is not yet enabled.  Committing in this state so it's available
        // and so the plugin is included in the list of plugins.

        if (PLUGIN_DISABLED) {
            return super.instanceOf(input, expectedType);
        }
        LiteralFactory lf = ctxt.getLiteralFactory();
        ctxt.info("Lowering instanceof:" + expectedType.getClass());
        // Value result = super.instanceOf(input, expectedType);
        // convert InstanceOf into a new FunctionCall()
        // RuntimeHelpers.fast_instanceof(CurrentThread, Value, ValueType) {
        //  cheap checks for class depth and then probe supers[]
        //  for array cases, etc, call RuntimeHelpers.slow_instanceOf(CurrentThread, Value, ValueType)
        // and let the optimizer inline the 'fast_instanceof' call and hope the rest is removed
        // mark the slow path as @noinline
        // DelegatingBasicBlockBuilder.getLocation() to get the bci & line
        MethodElement methodElement = ctxt.getVMHelperMethod("fast_instanceof");
        ctxt.registerEntryPoint(methodElement);
        Function function = ctxt.getExactFunction(methodElement);
        List<Value> args = List.of(input, lf.literalOfType(expectedType));
        return super.callFunction(lf.literalOfSymbol(function.getName(), function.getType()), args);
    }

    public Value classOf(final Value typeId) {
        ctxt.warning(getLocation(),"Lowering classOf to incomplete VMHelper stub");

        MethodElement methodElement = ctxt.getVMHelperMethod("classof_from_typeid");
        List<Value> args = List.of(typeId);
        return getFirstBuilder().invokeValueStatic(methodElement, args);
    }

    private boolean effectivelyFinal(ObjectType type) {
        if (type instanceof PrimitiveArrayObjectType) {
            return true;
        }
        if (type instanceof ReferenceArrayObjectType || type instanceof InterfaceObjectType) {
            return false;
        }
        // A class is effectively final if it has no live subclasses.
        ValidatedTypeDefinition vtd = type.getDefinition().validate();
        final int typeId = vtd.getTypeId();
        final int maxSubId = vtd.getMaximumSubtypeId();
        return typeId == maxSubId;
    }
}
