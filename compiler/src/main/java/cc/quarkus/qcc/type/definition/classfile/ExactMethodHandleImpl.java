package cc.quarkus.qcc.type.definition.classfile;

import java.nio.ByteBuffer;
import java.util.List;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ConstructorResolver;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.InitializerResolver;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.MethodResolver;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.InvokableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterElement;

/**
 *
 */
abstract class ExactMethodHandleImpl extends AbstractBufferBacked implements MethodHandle {
    private final ClassFileImpl classFile;
    private final int modifiers;
    final int index;
    final DefinedTypeDefinition enclosing;
    private final ByteBuffer byteCode;
    private final int maxStack;
    private final int maxLocals;
    private volatile MethodBody previous;
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

    public void replaceMethodBody(final MethodBody newBody) {
        MethodBody resolved = this.resolved;
        if (resolved != null) {
            previous = resolved;
        }
        this.resolved = newBody;
    }

    abstract ExecutableElement resolveElement();

    public MethodBody getMethodBody() {
        return resolved;
    }

    public MethodBody getPreviousMethodBody() {
        return previous;
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
            ExecutableElement element = resolveElement();
            BasicBlockBuilder gf = enclosing.getContext().newBasicBlockBuilder(element);
            MethodParser methodParser = new MethodParser(enclosing.getContext(), classMethodInfo, byteCode, gf);
            Value thisValue;
            Value[] parameters;
            if (element instanceof InvokableElement) {
                List<ParameterElement> elementParameters = ((InvokableElement) element).getParameters();
                int paramCount = elementParameters.size();
                parameters = new Value[paramCount];
                int j = 0;
                if ((modifiers & ClassFile.ACC_STATIC) == 0) {
                    // instance method or constructor
                    thisValue = gf.receiver(enclosing.validate().getType());
                    methodParser.setLocal(j++, thisValue);
                } else {
                    thisValue = null;
                }
                for (int i = 0; i < paramCount; i ++) {
                    ValueType type = elementParameters.get(i).getType(List.of());
                    parameters[i] = gf.parameter(type, i);
                    boolean class2 = elementParameters.get(i).hasClass2Type();
                    methodParser.setLocal(j, class2 ? methodParser.fatten(parameters[i]) : parameters[i]);
                    j += class2 ? 2 : 1;
                }
            } else {
                thisValue = null;
                parameters = Value.NO_VALUES;
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
            return this.resolved = this.previous = MethodBody.of(entryBlock, schedule, thisValue, parameters);
        }
    }

    static final class Constructor extends ExactMethodHandleImpl {
        private final ConstructorResolver resolver;

        Constructor(final ClassFileImpl classFile, final int modifiers, final int index, final ByteBuffer codeAttr, final DefinedTypeDefinition enclosing, final ConstructorResolver resolver) {
            super(classFile, modifiers, index, codeAttr, enclosing);
            this.resolver = resolver;
        }

        ConstructorElement resolveElement() {
            return resolver.resolveConstructor(index, enclosing);
        }
    }

    static final class Initializer extends ExactMethodHandleImpl {
        private final InitializerResolver resolver;

        Initializer(final ClassFileImpl classFile, final int modifiers, final int index, final ByteBuffer codeAttr, final DefinedTypeDefinition enclosing, final InitializerResolver resolver) {
            super(classFile, modifiers, index, codeAttr, enclosing);
            this.resolver = resolver;
        }

        InitializerElement resolveElement() {
            return resolver.resolveInitializer(index, enclosing);
        }
    }

    static final class Method extends ExactMethodHandleImpl {
        private final MethodResolver resolver;

        Method(final ClassFileImpl classFile, final int modifiers, final int index, final ByteBuffer codeAttr, final DefinedTypeDefinition enclosing, final MethodResolver resolver) {
            super(classFile, modifiers, index, codeAttr, enclosing);
            this.resolver = resolver;
        }

        MethodElement resolveElement() {
            return resolver.resolveMethod(index, enclosing);
        }
    }
}
