package cc.quarkus.qcc.graph;

import static cc.quarkus.qcc.type.definition.classfile.ClassFile.*;

/**
 *
 */
public interface CommutativeBinaryValue extends BinaryValue {
    Kind getKind();
    void setKind(Kind kind);

    default Type getType() {
        switch (getKind()) {
            case CMP_EQ:
            case CMP_NE: {
                return Type.BOOL;
            }
            default: {
                return getLeftInput().getType();
            }
        }
    }

    enum Kind {
        ADD,
        MULTIPLY,
        AND,
        OR,
        XOR,
        CMP_EQ,
        CMP_NE,
        ;

        public static Kind fromOpcode(int opcode) {
            switch (opcode) {
                case OP_LADD:
                case OP_FADD:
                case OP_DADD:
                case OP_IADD: {
                    return ADD;
                }
                case OP_LAND:
                case OP_IAND: {
                    return AND;
                }
                case OP_LOR:
                case OP_IOR: {
                    return OR;
                }
                case OP_LXOR:
                case OP_IXOR: {
                    return XOR;
                }
                case OP_IFEQ:
                case OP_IFNULL:
                case OP_IF_ACMPEQ:
                case OP_IF_ICMPEQ: {
                    return CMP_EQ;
                }
                case OP_IFNE:
                case OP_IFNONNULL:
                case OP_IF_ACMPNE:
                case OP_IF_ICMPNE: {
                    return CMP_NE;
                }
                case OP_IMUL:
                case OP_LMUL:
                case OP_FMUL:
                case OP_DMUL: {
                    return MULTIPLY;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
