package cc.quarkus.qcc.graph2;

import org.objectweb.asm.Opcodes;

/**
 *
 */
public interface NonCommutativeBinaryValue extends BinaryValue {
    Kind getKind();
    void setKind(Kind kind);

    enum Kind {
        UNSIGNED_SHR,
        SHR,
        SHL,
        SUB,
        DIV,
        MOD,
        CMP_LT,
        CMP_GT,
        CMP_LE,
        CMP_GE,
        ;

        public static Kind fromOpcode(final int opcode) {
            switch (opcode) {
                case Opcodes.IUSHR:
                case Opcodes.LUSHR: {
                    return UNSIGNED_SHR;
                }
                case Opcodes.ISHR:
                case Opcodes.LSHR: {
                    return SHR;
                }
                case Opcodes.ISHL:
                case Opcodes.LSHL: {
                    return SHL;
                }
                case Opcodes.IFLT:
                case Opcodes.IF_ICMPLT: {
                    return CMP_LT;
                }
                case Opcodes.IFLE:
                case Opcodes.IF_ICMPLE: {
                    return CMP_LE;
                }
                case Opcodes.IFGT:
                case Opcodes.IF_ICMPGT: {
                    return CMP_GT;
                }
                case Opcodes.IFGE:
                case Opcodes.IF_ICMPGE: {
                    return CMP_GE;
                }
                case Opcodes.IDIV:
                case Opcodes.LDIV:
                case Opcodes.DDIV:
                case Opcodes.FDIV: {
                    return DIV;
                }
                case Opcodes.IREM:
                case Opcodes.LREM:
                case Opcodes.DREM:
                case Opcodes.FREM: {
                    return MOD;
                }
                case Opcodes.ISUB:
                case Opcodes.LSUB:
                case Opcodes.FSUB:
                case Opcodes.DSUB: {
                    return SUB;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
