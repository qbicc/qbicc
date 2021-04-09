package org.qbicc.type.annotation.type;

import static org.qbicc.type.annotation.type.TypeAnnotation.*;

/**
 * The possible target types of a type annotation.
 */
public final class TargetTypes {
    private TargetTypes() {}

    // these constants have to be here (rather than on TargetType) to avoid a possible class loading deadlock

    public static final TargetInfo.TypeParameter TYPE_TYPE_PARAMETER = new TargetInfo.TypeParameter();
    public static final TargetInfo.TypeParameter METHOD_TYPE_PARAMETER = new TargetInfo.TypeParameter();
    public static final TargetInfo.SuperType EXTENDS = new TargetInfo.SuperType();
    public static final TargetInfo.TypeParameterBound TYPE_TYPE_PARAMETER_BOUND = new TargetInfo.TypeParameterBound();
    public static final TargetInfo.TypeParameterBound METHOD_TYPE_PARAMETER_BOUND = new TargetInfo.TypeParameterBound();
    public static final TargetInfo.Empty FIELD_TYPE = new TargetInfo.Empty();
    public static final TargetInfo.Empty METHOD_RETURN_TYPE = new TargetInfo.Empty();
    public static final TargetInfo.Empty RECEIVER_TYPE = new TargetInfo.Empty();
    public static final TargetInfo.FormalParameter FORMAL_PARAMETER = new TargetInfo.FormalParameter();
    public static final TargetInfo.Throws THROWS = new TargetInfo.Throws();

    public static final TargetInfo.LocalVar LOCAL_VAR = new TargetInfo.LocalVar();
    public static final TargetInfo.LocalVar RESOURCE_VAR = new TargetInfo.LocalVar();
    public static final TargetInfo.Catch EXCEPTION_PARAMETER = new TargetInfo.Catch();
    public static final TargetInfo.Offset INSTANCEOF_EXPR = new TargetInfo.Offset();
    public static final TargetInfo.Offset NEW_EXPR = new TargetInfo.Offset();
    public static final TargetInfo.Offset NEW_METHOD_REF = new TargetInfo.Offset();
    public static final TargetInfo.Offset IDENTIFIER_METHOD_REF = new TargetInfo.Offset();
    public static final TargetInfo.TypeArgument CAST_EXPR = new TargetInfo.TypeArgument();
    public static final TargetInfo.TypeArgument CONSTRUCTOR_INV = new TargetInfo.TypeArgument();
    public static final TargetInfo.TypeArgument METHOD_INV = new TargetInfo.TypeArgument();
    public static final TargetInfo.TypeArgument CONSTRUCTOR_METHOD_REF = new TargetInfo.TypeArgument();
    public static final TargetInfo.TypeArgument METHOD_METHOD_REF = new TargetInfo.TypeArgument();

    public static TargetInfo getTargetInfo(int id) {
        switch (id) {
            case 0x00: return TYPE_TYPE_PARAMETER;
            case 0x01: return METHOD_TYPE_PARAMETER;
            case 0x10: return EXTENDS;
            case 0x11: return TYPE_TYPE_PARAMETER_BOUND;
            case 0x12: return METHOD_TYPE_PARAMETER_BOUND;
            case 0x13: return FIELD_TYPE;
            case 0x14: return METHOD_RETURN_TYPE;
            case 0x15: return RECEIVER_TYPE;
            case 0x16: return FORMAL_PARAMETER;
            case 0x17: return THROWS;

            case 0x40: return LOCAL_VAR;
            case 0x41: return RESOURCE_VAR;
            case 0x42: return EXCEPTION_PARAMETER;
            case 0x43: return INSTANCEOF_EXPR;
            case 0x44: return NEW_EXPR;
            case 0x45: return NEW_METHOD_REF;
            case 0x46: return IDENTIFIER_METHOD_REF;
            case 0x47: return CAST_EXPR;
            case 0x48: return CONSTRUCTOR_INV;
            case 0x49: return METHOD_INV;
            case 0x4a: return CONSTRUCTOR_METHOD_REF;
            case 0x4b: return METHOD_METHOD_REF;

            default: {
                throw parseError();
            }
        }
    }
}
