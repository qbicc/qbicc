package cc.quarkus.qcc.type.definition.classfile;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;

/**
 *
 */
final class ExactMethodHandleImpl extends AbstractBufferBacked implements MethodHandle {
    ExactMethodHandleImpl(final int modifiers, final int index, final ByteBuffer codeAttr) {
        super(codeAttr);
        int save = codeAttr.position();
        int maxStack = codeAttr.getShort() & 0xffff;
        int maxLocals = codeAttr.getShort() & 0xffff;
        int codeLen = codeAttr.getInt();
        int codeOffs = codeAttr.position();
        codeAttr.position(codeOffs + codeLen);
        int exTableLen = codeAttr.getShort() & 0xffff;
        int exTableOffs = codeAttr.position();
        codeAttr.position(exTableOffs + (exTableLen << 3));
        int attrCnt = codeAttr.getShort() & 0xffff;

    }

    public int getModifiers() {
        return 0;
    }

    public int getParameterCount() {
        return 0;
    }

    public MethodBody getResolvedMethodBody() throws ResolutionFailedException {
        return null;
    }
}
