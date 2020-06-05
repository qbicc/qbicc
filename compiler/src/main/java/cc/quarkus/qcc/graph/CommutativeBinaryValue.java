package cc.quarkus.qcc.graph;

import org.objectweb.asm.Opcodes;

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
                case Opcodes.LADD:
                case Opcodes.FADD:
                case Opcodes.DADD:
                case Opcodes.IADD: {
                    return ADD;
                }
                case Opcodes.LAND:
                case Opcodes.IAND: {
                    return AND;
                }
                case Opcodes.LOR:
                case Opcodes.IOR: {
                    return OR;
                }
                case Opcodes.LXOR:
                case Opcodes.IXOR: {
                    return XOR;
                }
                case Opcodes.IFEQ:
                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ICMPEQ: {
                    return CMP_EQ;
                }
                case Opcodes.IFNE:
                case Opcodes.IF_ACMPNE:
                case Opcodes.IF_ICMPNE: {
                    return CMP_NE;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
