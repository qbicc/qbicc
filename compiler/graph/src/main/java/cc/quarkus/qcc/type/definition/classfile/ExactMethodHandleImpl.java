package cc.quarkus.qcc.type.definition.classfile;

import static cc.quarkus.qcc.graph.FatValue.*;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
final class ExactMethodHandleImpl extends AbstractBufferBacked implements MethodHandle {
    private final ClassFileImpl classFile;
    private final int modifiers;
    private final int index;
    private final DefinedTypeDefinition enclosing;
    private final ByteBuffer byteCode;
    private final int maxStack;
    private final int maxLocals;
    private volatile MethodBody resolved;

    ExactMethodHandleImpl(final ClassFileImpl classFile, final int modifiers, final int index, final ByteBuffer codeAttr, final DefinedTypeDefinition enclosing) {
        super(codeAttr);
        this.classFile = classFile;
        this.modifiers = modifiers;
        this.index = index;
        this.enclosing = enclosing;
        int save = codeAttr.position();
        maxStack = codeAttr.getShort() & 0xffff;
        maxLocals = codeAttr.getShort() & 0xffff;
        int codeLen = codeAttr.getInt();
        int codeOffs = codeAttr.position();
        int lim = codeAttr.limit();
        codeAttr.limit(codeOffs + codeLen);
        byteCode = codeAttr.slice();
        codeAttr.limit(lim);
        codeAttr.position(codeOffs + codeLen);
        int exTableLen = codeAttr.getShort() & 0xffff;
        int exTableOffs = codeAttr.position();
        codeAttr.position(exTableOffs + (exTableLen << 3));
        int attrCnt = codeAttr.getShort() & 0xffff;
        codeAttr.position(save);
    }

    public int getModifiers() {
        return modifiers;
    }

    public int getParameterCount() {
        return classFile.resolveMethod(index, enclosing).getParameterCount();
    }

    public MethodBody createMethodBody() throws ResolutionFailedException {
        MethodBody resolved = this.resolved;
        if (resolved != null) {
            return resolved;
        }
        synchronized (this) {
            resolved = this.resolved;
            if (resolved != null) {
                return resolved;
            }
            ClassMethodInfo classMethodInfo = new ClassMethodInfo(classFile, modifiers, index, getBackingBuffer().duplicate());
            MethodElement methodElement = classFile.resolveMethod(index, enclosing);
            int paramCount = methodElement.getParameterCount();
            BasicBlockBuilder gf = enclosing.getContext().newBasicBlockBuilder();
            MethodParser methodParser = new MethodParser(enclosing.getContext(), classMethodInfo, gf);
            Value[] parameters = new Value[paramCount];
            int j = 0;
            Value thisValue;
            if ((modifiers & ClassFile.ACC_STATIC) == 0) {
                // instance method or constructor
                thisValue = gf.receiver(enclosing.validate().getTypeId());
                methodParser.setLocal(j++, thisValue);
            } else {
                thisValue = null;
            }
            for (int i = 0; i < paramCount; i ++) {
                ValueType type = methodElement.getParameter(i).getType();
                if (type.isClass2Type()) {
                    methodParser.setLocal(++j, parameters[i] = fatten(gf.parameter(type, i)));
                    j+=2;
                } else {
                    methodParser.setLocal(j, parameters[i] = gf.parameter(type, i));
                    j++;
                }
            }
            BlockLabel entryBlockHandle = new BlockLabel();
            gf.begin(entryBlockHandle);
            methodParser.processNewBlock(byteCode);
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryBlockHandle);
            Schedule schedule = Schedule.forMethod(entryBlock);
            return this.resolved = MethodBody.of(entryBlock, schedule, thisValue, parameters);
        }
    }
}
