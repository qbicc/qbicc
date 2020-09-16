package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 * An invocation on an object instance.
 */
public interface InstanceInvocation extends InstanceOperation, Invocation {
    Kind getKind();

    void setKind(Kind kind);

    default int getValueDependencyCount() {
        return Invocation.super.getValueDependencyCount() + 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : Invocation.super.getValueDependency(index - 1);
    }

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
    }
}
