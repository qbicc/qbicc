package cc.quarkus.qcc.type.definition.classfile;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.graph.Type;

final class MethodParser {
    private final ClassFileImpl classFile;
    private final int modifiers;
    private final int index;
    private final ByteBuffer codeAttr;
    private final Type[] stack;
    private int sp;
    private final Type[] locals;
    private int lp;
    private short[] entryPoints; // format: destination source (alternating), sorted by destination
    private int epSize;

    MethodParser(final ClassFileImpl classFile, final int modifiers, final int index, final ByteBuffer codeAttr) {
        this.classFile = classFile;
        this.modifiers = modifiers;
        this.index = index;
        this.codeAttr = codeAttr;
        int save = codeAttr.position();
        int maxStack = codeAttr.getShort() & 0xffff;
        stack = new Type[maxStack];
        int maxLocals = codeAttr.getShort() & 0xffff;
        locals = new Type[maxLocals];
        int codeLen = codeAttr.getInt();
        int codeOffs = codeAttr.position();
        codeAttr.position(codeOffs + codeLen);
        int exTableLen = codeAttr.getShort() & 0xffff;
        int exTableOffs = codeAttr.position();
        codeAttr.position(exTableOffs + (exTableLen << 3));
        int attrCnt = codeAttr.getShort() & 0xffff;
        int attrOffs = codeAttr.position();
        codeAttr.position(save);
    }


}
