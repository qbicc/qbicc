package cc.quarkus.qcc.graph;

import static cc.quarkus.qcc.type.definition.classfile.ClassFile.*;

/**
 *
 */
public interface NonCommutativeBinaryValue extends BinaryValue {
    Kind getKind();
    void setKind(Kind kind);

    default Type getType() {
        switch (getKind()) {
            case CMP_LT:
            case CMP_GT:
            case CMP_LE:
            case CMP_GE: {
                return Type.BOOL;
            }
            default: {
                return getLeftInput().getType();
            }
        }
    }

    enum Kind {
        SHR,
        SHL,
        SUB,
        DIV,
        MOD,
        CMP_LT,
        CMP_GT,
        CMP_LE,
        CMP_GE,
        ROL,
        ROR,
        ;

        public static Kind fromOpcode(final int opcode) {
            switch (opcode) {
                case OP_IUSHR:
                case OP_LUSHR:
                case OP_ISHR:
                case OP_LSHR: {
                    return SHR;
                }
                case OP_ISHL:
                case OP_LSHL: {
                    return SHL;
                }
                case OP_IFLT:
                case OP_IF_ICMPLT: {
                    return CMP_LT;
                }
                case OP_IFLE:
                case OP_IF_ICMPLE: {
                    return CMP_LE;
                }
                case OP_IFGT:
                case OP_IF_ICMPGT: {
                    return CMP_GT;
                }
                case OP_IFGE:
                case OP_IF_ICMPGE: {
                    return CMP_GE;
                }
                case OP_IDIV:
                case OP_LDIV:
                case OP_DDIV:
                case OP_FDIV: {
                    return DIV;
                }
                case OP_IREM:
                case OP_LREM:
                case OP_DREM:
                case OP_FREM: {
                    return MOD;
                }
                case OP_ISUB:
                case OP_LSUB:
                case OP_FSUB:
                case OP_DSUB: {
                    return SUB;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
