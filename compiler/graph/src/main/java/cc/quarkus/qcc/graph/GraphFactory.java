package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;

/**
 * A program graph factory.
 */
public interface GraphFactory {

    // values

    Value receiver(ClassType type);

    Value parameter(Type type, int index);

    Value catch_(Context ctxt, ClassType type);

    // phi

    PhiValue phi(Context ctxt, Type type);

    // ternary

    Value if_(Context ctxt, Value condition, Value trueValue, Value falseValue);

    // binary

    Value add(Context ctxt, Value v1, Value v2);

    Value multiply(Context ctxt, Value v1, Value v2);

    Value and(Context ctxt, Value v1, Value v2);

    Value or(Context ctxt, Value v1, Value v2);

    Value xor(Context ctxt, Value v1, Value v2); // also used for ones-complement

    Value cmpEq(Context ctxt, Value v1, Value v2);

    Value cmpNe(Context ctxt, Value v1, Value v2);

    Value shr(Context ctxt, Value v1, Value v2);

    Value shl(Context ctxt, Value v1, Value v2);

    Value sub(Context ctxt, Value v1, Value v2); // also used for twos-complement

    Value divide(Context ctxt, Value v1, Value v2);

    Value remainder(Context ctxt, Value v1, Value v2);

    Value cmpLt(Context ctxt, Value v1, Value v2);

    Value cmpGt(Context ctxt, Value v1, Value v2);

    Value cmpLe(Context ctxt, Value v1, Value v2);

    Value cmpGe(Context ctxt, Value v1, Value v2);

    Value rol(Context ctxt, Value v1, Value v2);

    Value ror(Context ctxt, Value v1, Value v2);

    // unary

    Value negate(Context ctxt, Value v); // neg is only needed for FP; ints should use 0-n

    Value byteSwap(Context ctxt, Value v);

    Value bitReverse(Context ctxt, Value v);

    Value countLeadingZeros(Context ctxt, Value v);

    Value countTrailingZeros(Context ctxt, Value v);

    Value populationCount(Context ctxt, Value v);

    Value arrayLength(Context ctxt, Value array);

    // typed

    Value instanceOf(Context ctxt, Value value, ClassType type);

    Value truncate(Context ctxt, Value value, WordType toType);

    Value extend(Context ctxt, Value value, WordType toType);

    Value bitCast(Context ctxt, Value value, WordType toType);

    Value valueConvert(Context ctxt, Value value, WordType toType);

    // memory

    Value new_(Context ctxt, ClassType type);

    Value newArray(Context ctxt, ArrayType type, Value size);

    Value multiNewArray(Context ctxt, ArrayType type, Value... dimensions);

    Value clone(Context ctxt, Value object);

    Value pointerLoad(Context ctxt, Value pointer, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    Value readInstanceField(Context ctxt, Value instance, FieldElement fieldElement, JavaAccessMode mode);

    Value readStaticField(Context ctxt, FieldElement fieldElement, JavaAccessMode mode);

    Value readArrayValue(Context ctxt, Value array, Value index, JavaAccessMode mode);

    Node pointerStore(Context ctxt, Value pointer, Value value, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    Node writeInstanceField(Context ctxt, Value instance, FieldElement fieldElement, Value value, JavaAccessMode mode);

    Node writeStaticField(Context ctxt, FieldElement fieldElement, Value value, JavaAccessMode mode);

    Node writeArrayValue(Context ctxt, Value array, Value index, Value value, JavaAccessMode mode);

    Node fence(Context ctxt, MemoryAtomicityMode fenceType);

    Node monitorEnter(Context ctxt, Value obj);

    Node monitorExit(Context ctxt, Value obj);

    // method invocation

    Node invokeStatic(Context ctxt, MethodElement target, List<Value> arguments);

    Node invokeInstance(Context ctxt, DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments);

    Value invokeValueStatic(Context ctxt, MethodElement target, List<Value> arguments);

    Value invokeInstanceValueMethod(Context ctxt, Value instance, DispatchInvocation.Kind kind, MethodElement target, List<Value> arguments);

    /**
     * Invoke an object instance initializer.  The value returned has an initialized type.  The returned value should
     * replace all occurrences of the uninitialized value when processing bytecode.
     *
     * @param ctxt the current context (must not be {@code null})
     * @param instance the uninitialized instance to initialize (must not be {@code null})
     * @param target the constructor to invoke (must not be {@code null})
     * @param arguments the constructor arguments (must not be {@code null})
     * @return the initialized value
     */
    Value invokeConstructor(Context ctxt, Value instance, ConstructorElement target, List<Value> arguments);

    // misc

    /**
     * Begin a new block.  The returned node will be a dependency (usually the topmost dependency) of the terminator.
     *
     * @param ctxt the current context (must not be {@code null})
     * @param blockLabel the label of the new block (must not be {@code null} or resolved)
     * @return the node representing the block entry
     */
    Node begin(Context ctxt, BlockLabel blockLabel);

    // control flow - terminalBlock is updated to point to this terminator

    /**
     * Generate a {@code goto} termination node.  The terminated block is returned.
     *
     * @param ctxt the current context (must not be {@code null})
     * @param resumeLabel the handle of the jump target (must not be {@code null})
     * @return the terminated block
     */
    BasicBlock goto_(Context ctxt, BlockLabel resumeLabel);

    /**
     * Construct an {@code if} node.  If the condition is true, the {@code trueTarget} will receive control.  Otherwise,
     * the {@code falseTarget} will receive control.
     * <p>
     * Terminates the current block, which is returned.
     *
     * @param ctxt the context (must not be {@code null})
     * @param condition the condition (must not be {@code null})
     * @param trueTarget the execution target to use when {@code condition} is {@code true}
     * @param falseTarget the execution target to use when {@code condition} is {@code false}
     * @return the terminated block
     */
    BasicBlock if_(Context ctxt, Value condition, BlockLabel trueTarget, BlockLabel falseTarget);

    BasicBlock return_(Context ctxt);

    BasicBlock return_(Context ctxt, Value value);

    BasicBlock throw_(Context ctxt, Value value);

    BasicBlock switch_(Context ctxt, Value value, int[] checkValues, BlockLabel[] targets, BlockLabel defaultTarget);

    /**
     * Construct a {@code jsr} node which must be returned from.  Before lowering, {@code jsr} nodes are inlined,
     * copying all of the nodes into new basic blocks.
     * <p>
     * Terminates the current block.
     *
     * @param ctxt the context (must not be {@code null})
     * @param subLabel the subroutine call target (must not be {@code null})
     * @param resumeLabel the block to return to (must not be {@code null})
     * @return the terminated block
     */
    BasicBlock jsr(Context ctxt, BlockLabel subLabel, final BlockLabel resumeLabel);

    /**
     * Return from a {@code jsr} subroutine call.
     * <p>
     * Terminates the current block.
     *
     * @param ctxt the context (must not be {@code null})
     * @param address the return address (must not be {@code null})
     * @return the node
     */
    BasicBlock ret(Context ctxt, Value address);

    /**
     * A basic factory which produces each kind of node.
     */
    GraphFactory BASIC_FACTORY = new GraphFactory() {
        public Value add(final Context ctxt, final Value v1, final Value v2) {
            return new Add(v1, v2);
        }

        public Value multiply(final Context ctxt, final Value v1, final Value v2) {
            return new Multiply(v1, v2);
        }

        public Value and(final Context ctxt, final Value v1, final Value v2) {
            return new And(v1, v2);
        }

        public Value or(final Context ctxt, final Value v1, final Value v2) {
            return new Or(v1, v2);
        }

        public Value xor(final Context ctxt, final Value v1, final Value v2) {
            return new Xor(v1, v2);
        }

        public Value cmpEq(final Context ctxt, final Value v1, final Value v2) {
            return new CmpEq(v1, v2);
        }

        public Value cmpNe(final Context ctxt, final Value v1, final Value v2) {
            return new CmpNe(v1, v2);
        }

        public Value shr(final Context ctxt, final Value v1, final Value v2) {
            return new Shr(v1, v2);
        }

        public Value shl(final Context ctxt, final Value v1, final Value v2) {
            return new Shl(v1, v2);
        }

        public Value sub(final Context ctxt, final Value v1, final Value v2) {
            return new Sub(v1, v2);
        }

        public Value divide(final Context ctxt, final Value v1, final Value v2) {
            return new Div(v1, v2);
        }

        public Value remainder(final Context ctxt, final Value v1, final Value v2) {
            return new Mod(v1, v2);
        }

        public Value cmpLt(final Context ctxt, final Value v1, final Value v2) {
            return new CmpLt(v1, v2);
        }

        public Value cmpGt(final Context ctxt, final Value v1, final Value v2) {
            return new CmpGt(v1, v2);
        }

        public Value cmpLe(final Context ctxt, final Value v1, final Value v2) {
            return new CmpLe(v1, v2);
        }

        public Value cmpGe(final Context ctxt, final Value v1, final Value v2) {
            return new CmpGe(v1, v2);
        }

        public Value rol(final Context ctxt, final Value v1, final Value v2) {
            return new Rol(v1, v2);
        }

        public Value ror(final Context ctxt, final Value v1, final Value v2) {
            return new Ror(v1, v2);
        }

        public Value negate(final Context ctxt, final Value v) {
            return new Neg(v);
        }

        public Value byteSwap(final Context ctxt, final Value v) {
            throw Assert.unsupported();
        }

        public Value bitReverse(final Context ctxt, final Value v) {
            throw Assert.unsupported();
        }

        public Value countLeadingZeros(final Context ctxt, final Value v) {
            throw Assert.unsupported();
        }

        public Value countTrailingZeros(final Context ctxt, final Value v) {
            throw Assert.unsupported();
        }

        public Value populationCount(final Context ctxt, final Value v) {
            throw Assert.unsupported();
        }

        public Value arrayLength(final Context ctxt, final Value array) {
            return new ArrayLength(array);
        }

        public Value truncate(final Context ctxt, final Value value, final WordType toType) {
            return new Truncate(value, toType);
        }

        public Value extend(final Context ctxt, final Value value, final WordType toType) {
            return new Extend(value, toType);
        }

        public Value bitCast(final Context ctxt, final Value value, final WordType toType) {
            return new BitCast(value, toType);
        }

        public Value valueConvert(final Context ctxt, final Value value, final WordType toType) {
            return new Convert(value, toType);
        }

        public Value receiver(final ClassType type) {
            return new ThisValue(type);
        }

        public Value parameter(final Type type, final int index) {
            return new ParameterValue(type, index);
        }

        public Value catch_(final Context ctxt, final ClassType type) {
            return new Catch(ctxt.getCurrentBlock(), type);
        }

        public PhiValue phi(final Context ctxt, final Type type) {
            return new PhiValue(type, ctxt.getCurrentBlock());
        }

        public Value if_(final Context ctxt, final Value condition, final Value trueValue, final Value falseValue) {
            return new Select(condition, trueValue, falseValue);
        }

        public Value instanceOf(final Context ctxt, final Value value, final ClassType type) {
            return new InstanceOf(value, type);
        }

        public Value new_(Context ctxt, final ClassType type) {
            return new New(ctxt, type);
        }

        public Value newArray(Context ctxt, final ArrayType type, final Value size) {
            return new NewArray(ctxt, type, size);
        }

        public Value multiNewArray(final Context ctxt, final ArrayType type, final Value... dimensions) {
            throw Assert.unsupported();
        }

        public Value clone(final Context ctxt, final Value object) {
            throw Assert.unsupported();
        }

        public Value pointerLoad(Context ctxt, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
            throw Assert.unsupported();
        }

        public Value readInstanceField(Context ctxt, final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
            return new InstanceFieldRead(ctxt, instance, fieldElement, mode);
        }

        public Value readStaticField(Context ctxt, final FieldElement fieldElement, final JavaAccessMode mode) {
            return new StaticFieldRead(ctxt, fieldElement, mode);
        }

        public Value readArrayValue(Context ctxt, final Value array, final Value index, final JavaAccessMode mode) {
            return new ArrayElementRead(ctxt, array, index, mode);
        }

        public Node pointerStore(Context ctxt, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
            throw Assert.unsupported();
        }

        public Node writeInstanceField(Context ctxt, final Value instance, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
            return new InstanceFieldWrite(ctxt, instance, fieldElement, value, mode);
        }

        public Node writeStaticField(Context ctxt, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
            return new StaticFieldWrite(ctxt, fieldElement, value, mode);
        }

        public Node writeArrayValue(Context ctxt, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
            return new ArrayElementWrite(ctxt, array, index, value, mode);
        }

        public Node fence(Context ctxt, final MemoryAtomicityMode fenceType) {
            throw Assert.unsupported();
        }

        public Node monitorEnter(final Context ctxt, final Value obj) {
            throw Assert.unsupported();
        }

        public Node monitorExit(final Context ctxt, final Value obj) {
            throw Assert.unsupported();
        }

        public Node invokeStatic(Context ctxt, final MethodElement target, final List<Value> arguments) {
            return new StaticInvocation(ctxt, target, arguments);
        }

        public Node invokeInstance(Context ctxt, final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
            return new InstanceInvocation(ctxt, kind, instance, target, arguments);
        }

        public Value invokeValueStatic(Context ctxt, final MethodElement target, final List<Value> arguments) {
            return new StaticInvocationValue(ctxt, target, arguments);
        }

        public Value invokeInstanceValueMethod(Context ctxt, final Value instance, final DispatchInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
            return new InstanceInvocationValue(ctxt, kind, instance, target, arguments);
        }

        public Value invokeConstructor(final Context ctxt, final Value instance, final ConstructorElement target, final List<Value> arguments) {
            return new ConstructorInvocation(ctxt, instance, target, arguments);
        }

        public Node begin(final Context ctxt, final BlockLabel blockLabel) {
            Assert.checkNotNullParam("ctxt", ctxt);
            Assert.checkNotNullParam("blockLabel", blockLabel);
            if (blockLabel.hasTarget()) {
                throw new IllegalStateException("Block already terminated");
            }
            ctxt.setNewCurrentBlock(blockLabel);
            return new BlockEntry(blockLabel);
        }

        public BasicBlock goto_(Context ctxt, final BlockLabel resumeLabel) {
            BlockLabel block = ctxt.getCurrentBlock();
            Goto.create(ctxt, resumeLabel);
            return BlockLabel.getTargetOf(block);
        }

        public BasicBlock if_(Context ctxt, final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
            BlockLabel block = ctxt.getCurrentBlock();
            If.create(ctxt, condition, trueTarget, falseTarget);
            return BlockLabel.getTargetOf(block);
        }

        public BasicBlock return_(Context ctxt) {
            BlockLabel block = ctxt.getCurrentBlock();
            Return.create(ctxt);
            return BlockLabel.getTargetOf(block);
        }

        public BasicBlock return_(Context ctxt, final Value value) {
            BlockLabel block = ctxt.getCurrentBlock();
            ValueReturn.create(ctxt, value);
            return BlockLabel.getTargetOf(block);
        }

        public BasicBlock throw_(Context ctxt, final Value value) {
            BlockLabel block = ctxt.getCurrentBlock();
            Throw.create(ctxt, value);
            return BlockLabel.getTargetOf(block);
        }

        public BasicBlock jsr(final Context ctxt, final BlockLabel subLabel, final BlockLabel resumeLabel) {
            BlockLabel block = ctxt.getCurrentBlock();
            Jsr.create(ctxt, subLabel, resumeLabel);
            return BlockLabel.getTargetOf(block);
        }

        public BasicBlock ret(final Context ctxt, final Value address) {
            BlockLabel block = ctxt.getCurrentBlock();
            Ret.create(ctxt, address);
            return BlockLabel.getTargetOf(block);
        }

        public BasicBlock switch_(Context ctxt, final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget) {
            BlockLabel block = ctxt.getCurrentBlock();
            Switch.create(ctxt, value, checkValues, targets, defaultTarget);
            return BlockLabel.getTargetOf(block);
        }
    };

    /**
     * Basic block building context for node dependency management, try/catch management, and any other intermediate state.
     */
    final class Context implements Cloneable {
        private Node dependency;
        private BlockLabel currentBlock;

        public Context() {
            currentBlock = new BlockLabel();
        }

        public Context(final BlockLabel currentBlock) {
            this.currentBlock = currentBlock;
        }

        public Node getDependency() {
            return dependency;
        }

        public Node setDependency(final Node dependency) {
            try {
                return this.dependency;
            } finally {
                this.dependency = Assert.checkNotNullParam("dependency", dependency);
            }
        }

        public Context clone() {
            try {
                return (Context) super.clone();
            } catch (CloneNotSupportedException e) {
                // impossible
                throw new IllegalStateException(e);
            }
        }

        public BlockLabel getCurrentBlock() {
            BlockLabel currentBlock = this.currentBlock;
            if (currentBlock == null) {
                throw new IllegalStateException("No current block");
            }
            return currentBlock;
        }

        public void setCurrentBlock(final BlockLabel currentBlock) {
            dependency = null;
            this.currentBlock = currentBlock;
        }

        void setNewCurrentBlock(final BlockLabel currentBlock) {
            BlockLabel oldBlock = this.currentBlock;
            if (oldBlock != null) {
                throw new IllegalStateException("Current block unterminated");
            }
            this.currentBlock = currentBlock;
        }

        public BlockLabel getAndSetCurrentBlock(final BlockLabel currentBlock) {
            try {
                return getCurrentBlock();
            } finally {
                this.currentBlock = currentBlock;
            }
        }
    }
}
