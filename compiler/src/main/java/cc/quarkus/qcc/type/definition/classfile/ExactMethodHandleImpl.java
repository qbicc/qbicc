package cc.quarkus.qcc.type.definition.classfile;

import java.nio.ByteBuffer;
import java.util.List;

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
        return classFile.resolveMethod(index, enclosing).getParameters().size();
    }

    public void replaceMethodBody(final MethodBody newBody) {
        resolved = newBody;
    }

    public MethodBody getOrCreateMethodBody() throws ResolutionFailedException {
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
            int paramCount = methodElement.getParameters().size();
            BasicBlockBuilder gf = enclosing.getContext().newBasicBlockBuilder(methodElement);
            MethodParser methodParser = new MethodParser(enclosing.getContext(), classMethodInfo, byteCode, gf);
            Value[] parameters = new Value[paramCount];
            int j = 0;
            Value thisValue;
            if ((modifiers & ClassFile.ACC_STATIC) == 0) {
                // instance method or constructor
                thisValue = gf.receiver(enclosing.validate().getType());
                methodParser.setLocal(j++, thisValue);
            } else {
                thisValue = null;
            }
            for (int i = 0; i < paramCount; i ++) {
                ValueType type = methodElement.getParameters().get(i).getType(List.of());
                parameters[i] = gf.parameter(type, i);
                boolean class2 = methodElement.getParameters().get(i).hasClass2Type();
                methodParser.setLocal(j, class2 ? methodParser.fatten(parameters[i]) : parameters[i]);
                j += class2 ? 2 : 1;
            }
            // process the main entry point
            BlockLabel entryBlockHandle = methodParser.getBlockForIndexIfExists(0);
            if (entryBlockHandle == null) {
                // no loop to start block; just process it as a new block
                entryBlockHandle = new BlockLabel();
                gf.begin(entryBlockHandle);
                byteCode.position(0);
                methodParser.processNewBlock();
            } else {
                // we have to jump into it because there is a loop that includes index 0
                byteCode.position(0);
                gf.begin(new BlockLabel());
                methodParser.processBlock(gf.goto_(entryBlockHandle));
            }
            gf.finish();
            BasicBlock entryBlock = BlockLabel.getTargetOf(entryBlockHandle);
            Schedule schedule = Schedule.forMethod(entryBlock);
            return this.resolved = MethodBody.of(entryBlock, schedule, thisValue, parameters);
        }
    }
}
