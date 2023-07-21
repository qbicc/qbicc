package org.qbicc.plugin.instanceofcheckcast;

import java.util.List;
import java.util.Map;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.NullableType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.MethodElement;

/**
 * A basic block builder that cleans up and trivializes invalid casts and instance checks, usable in all phases.
 */
public final class InvalidCastsCleanupBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    public InvalidCastsCleanupBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public Value instanceOf(Value input, ObjectType expectedType, int expectedDimensions) {
        if (input.getType() instanceof ReferenceType inputRefType) {
            if (input instanceof NullLiteral) {
                return getLiteralFactory().literalOf(false);
            }
            ObjectType ifTrueExpectedType = expectedType;
            for (int i=0; i<expectedDimensions; i++) {
                ifTrueExpectedType = ifTrueExpectedType.getReferenceArrayObject();
            }
            if (inputRefType.instanceOf(ifTrueExpectedType)) {
                return getLiteralFactory().literalOf(true);
            }
            ReferenceType narrowed = inputRefType.narrow(ifTrueExpectedType);
            if (narrowed == null) {
                return getLiteralFactory().literalOf(false);
            }
        }
        return super.instanceOf(input, expectedType, expectedDimensions);
    }

    @Override
    public Value checkcast(Value value, Value toType, Value toDimensions, CheckCast.CastType kind, ObjectType expectedType) {
        ValueType inputType = value.getType();
        if (inputType instanceof ReferenceType rt) {
            ReferenceType outputType = rt.narrow(expectedType);
            if (outputType == null) {
                // the cast is not possible
                if (value.isNullable()) {
                    // null check first
                    BlockLabel resume = new BlockLabel();
                    BlockLabel doThrow = new BlockLabel();
                    if_(isEq(value, getLiteralFactory().nullLiteralOfType(value.getType(NullableType.class))), resume, doThrow, Map.of());
                    begin(doThrow);
                    try {
                        MethodElement thrower = RuntimeMethodFinder.get(getContext()).getMethod(kind.equals(CheckCast.CastType.Cast) ? "raiseClassCastException" : "raiseArrayStoreException");
                        callNoReturn(getLiteralFactory().literalOf(thrower), List.of());
                    } catch (BlockEarlyTermination ignored) {}
                    begin(resume);
                    return getLiteralFactory().nullLiteralOfType(expectedType.getReference());
                } else {
                    // just throw
                    MethodElement thrower = RuntimeMethodFinder.get(getContext()).getMethod(kind.equals(CheckCast.CastType.Cast) ? "raiseClassCastException" : "raiseArrayStoreException");
                    throw new BlockEarlyTermination(callNoReturn(getLiteralFactory().literalOf(thrower), List.of()));
                }
            } else if (value instanceof NullLiteral) {
                // no operation
                return getLiteralFactory().nullLiteralOfType(outputType);
            } else {
                if (toType instanceof TypeIdLiteral && toDimensions instanceof IntegerLiteral) {
                    // We know the exact toType at compile time
                    ObjectType toTypeOT = (ObjectType) ((TypeIdLiteral) toType).getValue(); // by construction in MemberResolvingBasicBlockBuilder.checkcast
                    int dims = ((IntegerLiteral) toDimensions).intValue();
                    if (isAlwaysAssignable(rt, toTypeOT, dims)) {
                        return bitCast(value, outputType);
                    }
                } else if (kind.equals(CheckCast.CastType.ArrayStore) && isEffectivelyFinal(expectedType)) {
                    // We do not have toType as a compile-time literal, but since the element type of the array we are
                    // storing into is effectivelyFinal we do not need to worry about co-variant array subtyping.
                    if (rt.instanceOf(expectedType)) {
                        return bitCast(value, outputType);
                    }
                }
            }
        }
        // not trivially resolvable
        return super.checkcast(value, toType, toDimensions, kind, expectedType);
    }

    private boolean isAlwaysAssignable(ReferenceType inputType, ObjectType toType, int toDimensions) {
        if (toDimensions == 0) {
            return inputType.instanceOf(toType);
        } else if (inputType.getUpperBound() instanceof ReferenceArrayObjectType inputArrayType) {
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
        // we don't have the type IDs yet at this point, but we can look for a 'final' modifier.
        return type.getDefinition().load().isFinal();
    }
}
