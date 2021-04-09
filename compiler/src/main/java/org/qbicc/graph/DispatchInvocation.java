package org.qbicc.graph;

import java.util.Locale;

import org.qbicc.type.definition.classfile.ClassFile;

/**
 *
 */
public interface DispatchInvocation extends Invocation {
    Kind getKind();

    enum Kind {
        EXACT,
        VIRTUAL,
        INTERFACE,
        ;
        public static Kind fromOpcode(int opcode) {
            switch (opcode) {
                case ClassFile.OP_INVOKESPECIAL: return EXACT;
                case ClassFile.OP_INVOKEVIRTUAL: return VIRTUAL;
                case ClassFile.OP_INVOKEINTERFACE: return INTERFACE;
                default: {
                    throw new IllegalArgumentException("Unknown opcode " + opcode);
                }
            }
        }

        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
