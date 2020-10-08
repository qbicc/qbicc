package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;
import io.smallrye.common.constraint.Assert;

/**
 * A program graph factory.
 */
public interface GraphFactory {

    // values

    ThisValue receiver(ClassType type);

    ParameterValue parameter(Type type, int index);

    // phi

    PhiValue phi(Type type, BasicBlock basicBlock);

    PhiValue phi(Type type, NodeHandle basicBlockHandle);

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

    Value lengthOfArray(Context ctxt, Value array);

    // typed

    Value instanceOf(Context ctxt, Value v, ClassType type);

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

    Value readInstanceField(Context ctxt, Value instance, ClassType owner, String name, JavaAccessMode mode);

    Value readStaticField(Context ctxt, ClassType owner, String name, JavaAccessMode mode);

    Value readArrayValue(Context ctxt, Value array, Value index, JavaAccessMode mode);

    Node pointerStore(Context ctxt, Value pointer, Value value, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    Node writeInstanceField(Context ctxt, Value instance, ClassType owner, String name, Value value, JavaAccessMode mode);

    Node writeStaticField(Context ctxt, ClassType owner, String name, Value value, JavaAccessMode mode);

    Node writeArrayValue(Context ctxt, Value array, Value index, Value value, JavaAccessMode mode);

    Node fence(Context ctxt, MemoryAtomicityMode fenceType);

    Node monitorEnter(Context ctxt, Value obj);

    Node monitorExit(Context ctxt, Value obj);

    // method invocation

    Node invokeMethod(Context ctxt, ParameterizedExecutableElement target, List<Value> arguments);

    Node invokeInstanceMethod(Context ctxt, Value instance, InstanceInvocation.Kind kind, ParameterizedExecutableElement target, List<Value> arguments);

    Value invokeValueMethod(Context ctxt, MethodElement target, List<Value> arguments);

    Value invokeInstanceValueMethod(Context ctxt, Value instance, InstanceInvocation.Kind kind, MethodElement target, List<Value> arguments);

    // control flow - terminalBlock is updated to point to this terminator

    /**
     * Generate a {@code goto} termination node.  The terminated block is returned.
     *
     * @param ctxt the current context (must not be {@code null})
     * @param targetHandle the handle of the jump target (must not be {@code null})
     * @return the terminated block
     */
    BasicBlock goto_(Context ctxt, NodeHandle targetHandle);

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
    BasicBlock if_(Context ctxt, Value condition, NodeHandle trueTarget, NodeHandle falseTarget);

    BasicBlock return_(Context ctxt);

    BasicBlock return_(Context ctxt, Value value);

    BasicBlock throw_(Context ctxt, Value value);

    BasicBlock switch_(Context ctxt, Value value, int[] checkValues, NodeHandle[] targets, NodeHandle defaultTarget);

    /**
     * Construct a {@code jsr} node which must be returned from.  Before lowering, {@code jsr} nodes are inlined,
     * copying all of the nodes into new basic blocks.
     * <p>
     * Terminates the current block.
     *
     * @param ctxt the context (must not be {@code null})
     * @param target the subroutine call target (must not be {@code null})
     * @param ret the block to return to (must not be {@code null})
     * @return the terminated block
     */
    BasicBlock jsr(Context ctxt, NodeHandle target, final NodeHandle ret);

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

        // todo: separate interfaces/impl classes

        public Value add(final Context ctxt, final Value v1, final Value v2) {
            CommutativeBinaryValueImpl value = new CommutativeBinaryValueImpl();
            value.setKind(CommutativeBinaryValue.Kind.ADD);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value multiply(final Context ctxt, final Value v1, final Value v2) {
            CommutativeBinaryValueImpl value = new CommutativeBinaryValueImpl();
            value.setKind(CommutativeBinaryValue.Kind.MULTIPLY);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value and(final Context ctxt, final Value v1, final Value v2) {
            CommutativeBinaryValueImpl value = new CommutativeBinaryValueImpl();
            value.setKind(CommutativeBinaryValue.Kind.AND);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value or(final Context ctxt, final Value v1, final Value v2) {
            CommutativeBinaryValueImpl value = new CommutativeBinaryValueImpl();
            value.setKind(CommutativeBinaryValue.Kind.OR);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value xor(final Context ctxt, final Value v1, final Value v2) {
            CommutativeBinaryValueImpl value = new CommutativeBinaryValueImpl();
            value.setKind(CommutativeBinaryValue.Kind.XOR);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value cmpEq(final Context ctxt, final Value v1, final Value v2) {
            CommutativeBinaryValueImpl value = new CommutativeBinaryValueImpl();
            value.setKind(CommutativeBinaryValue.Kind.CMP_EQ);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value cmpNe(final Context ctxt, final Value v1, final Value v2) {
            CommutativeBinaryValueImpl value = new CommutativeBinaryValueImpl();
            value.setKind(CommutativeBinaryValue.Kind.CMP_NE);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value shr(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.SHR);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value shl(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.SHL);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value sub(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.SUB);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value divide(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.DIV);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value remainder(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.MOD);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value cmpLt(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.CMP_LT);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value cmpGt(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.CMP_GT);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value cmpLe(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.CMP_LE);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value cmpGe(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.CMP_GE);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value rol(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.ROL);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value ror(final Context ctxt, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(NonCommutativeBinaryValue.Kind.ROR);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value negate(final Context ctxt, final Value v) {
            UnaryValueImpl value = new UnaryValueImpl();
            value.setKind(UnaryValue.Kind.NEGATE);
            value.setInput(v);
            return value;
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

        public Value truncate(final Context ctxt, final Value value, final WordType toType) {
            WordCastValueImpl op = new WordCastValueImpl();
            op.setType(toType);
            op.setInput(value);
            op.setKind(WordCastValue.Kind.TRUNCATE);
            return op;
        }

        public Value extend(final Context ctxt, final Value value, final WordType toType) {
            WordCastValueImpl op = new WordCastValueImpl();
            op.setType(toType);
            op.setInput(value);
            op.setKind(WordCastValue.Kind.EXTEND);
            return op;
        }

        public Value bitCast(final Context ctxt, final Value value, final WordType toType) {
            WordCastValueImpl op = new WordCastValueImpl();
            op.setType(toType);
            op.setInput(value);
            op.setKind(WordCastValue.Kind.BIT_CAST);
            return op;
        }

        public Value valueConvert(final Context ctxt, final Value value, final WordType toType) {
            WordCastValueImpl op = new WordCastValueImpl();
            op.setType(toType);
            op.setInput(value);
            op.setKind(WordCastValue.Kind.VALUE_CONVERT);
            return op;
        }

        public Value populationCount(final Context ctxt, final Value v) {
            throw new UnsupportedOperationException();
        }

        public Value lengthOfArray(final Context ctxt, final Value array) {
            ArrayLengthValueImpl value = new ArrayLengthValueImpl();
            value.setInstance(array);
            return value;
        }

        public ThisValue receiver(final ClassType type) {
            return new ThisValueImpl(type);
        }

        public ParameterValue parameter(final Type type, final int index) {
            ParameterValueImpl value = new ParameterValueImpl();
            value.setType(type);
            value.setIndex(index);
            return value;
        }

        public PhiValue phi(final Type type, final BasicBlock basicBlock) {
            return phi(type, NodeHandle.of(basicBlock));
        }

        public PhiValue phi(final Type type, final NodeHandle basicBlockHandle) {
            PhiValueImpl phiValue = new PhiValueImpl(basicBlockHandle);
            phiValue.setType(type);
            return phiValue;
        }

        public Value if_(final Context ctxt, final Value condition, final Value trueValue, final Value falseValue) {
            IfValueImpl value = new IfValueImpl();
            value.setCond(condition);
            value.setTrueValue(trueValue);
            value.setFalseValue(falseValue);
            return value;
        }

        public Value instanceOf(final Context ctxt, final Value v, final ClassType type) {
            InstanceOfValueImpl value = new InstanceOfValueImpl();
            value.setInstance(v);
            value.setInstanceType(type);
            return value;
        }

        public Value new_(Context ctxt, final ClassType type) {
            NewValueImpl value = new NewValueImpl();
            value.setBasicDependency(ctxt.setDependency(value));
            value.setType(type);
            return value;
        }

        public Value newArray(Context ctxt, final ArrayType type, final Value size) {
            NewArrayValueImpl value = new NewArrayValueImpl();
            value.setBasicDependency(ctxt.setDependency(value));
            value.setType((ArrayClassType) type); // todo
            value.setSize(size);
            return value;
        }

        public Value multiNewArray(final Context ctxt, final ArrayType type, final Value... dimensions) {
            throw new UnsupportedOperationException("Multi new array");
        }

        public Value clone(final Context ctxt, final Value object) {
            throw new UnsupportedOperationException("Clone");
        }

        public Value pointerLoad(Context ctxt, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
            throw new UnsupportedOperationException("Pointers");
        }

        public Value readInstanceField(Context ctxt, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
            InstanceFieldReadValueImpl value = new InstanceFieldReadValueImpl();
            value.setBasicDependency(ctxt.setDependency(value));
            value.setInstance(instance);
            value.setFieldOwner(owner);
            value.setFieldName(name);
            value.setMode(mode);
            return value;
        }

        public Value readStaticField(Context ctxt, final ClassType owner, final String name, final JavaAccessMode mode) {
            StaticFieldReadValueImpl value = new StaticFieldReadValueImpl();
            value.setBasicDependency(ctxt.setDependency(value));
            value.setFieldOwner(owner);
            value.setFieldName(name);
            value.setMode(mode);
            return value;
        }

        public Value readArrayValue(Context ctxt, final Value array, final Value index, final JavaAccessMode mode) {
            ArrayElementReadValueImpl value = new ArrayElementReadValueImpl();
            value.setBasicDependency(ctxt.setDependency(value));
            value.setInstance(array);
            value.setIndex(index);
            value.setMode(mode);
            return value;
        }

        public Node pointerStore(Context ctxt, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
            throw new UnsupportedOperationException("Pointers");
        }

        public Node writeInstanceField(Context ctxt, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
            InstanceFieldWriteImpl op = new InstanceFieldWriteImpl();
            op.setBasicDependency(ctxt.setDependency(op));
            op.setInstance(instance);
            op.setFieldOwner(owner);
            op.setFieldName(name);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public Node writeStaticField(Context ctxt, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
            StaticFieldWriteImpl op = new StaticFieldWriteImpl();
            op.setBasicDependency(ctxt.setDependency(op));
            op.setFieldOwner(owner);
            op.setFieldName(name);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public Node writeArrayValue(Context ctxt, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
            ArrayElementWriteImpl op = new ArrayElementWriteImpl();
            op.setBasicDependency(ctxt.setDependency(op));
            op.setInstance(array);
            op.setIndex(index);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public Node fence(Context ctxt, final MemoryAtomicityMode fenceType) {
            throw new UnsupportedOperationException("Fence");
        }

        public Node monitorEnter(final Context ctxt, final Value obj) {
            throw new UnsupportedOperationException("Monitors");
        }

        public Node monitorExit(final Context ctxt, final Value obj) {
            throw new UnsupportedOperationException("Monitors");
        }

        public Node invokeMethod(Context ctxt, final ParameterizedExecutableElement target, final List<Value> arguments) {
            NodeHandle catch_ = ctxt.getCatch();
            InvocationImpl op;
            if (catch_ == null) {
                op = new InvocationImpl();
                op.setBasicDependency(ctxt.setDependency(op));
            } else {
                TryInvocationImpl tryOp = new TryInvocationImpl();
                tryOp.setBasicDependency(ctxt.setDependency(null));
                tryOp.setCatchHandler(catch_);
                // end the current block with the try/invoke, and start a new block
                NodeHandle nextBlock = new NodeHandle();
                ctxt.getAndSetCurrentBlock(nextBlock).setTarget(block(tryOp));
                tryOp.setNextBlock(nextBlock);
                op = tryOp;
            }
            op.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                op.setArgument(idx, argument);
            }
            op.setInvocationTarget(target);
            return op;
        }

        public Node invokeInstanceMethod(Context ctxt, final Value instance, final InstanceInvocation.Kind kind, final ParameterizedExecutableElement target, final List<Value> arguments) {
            NodeHandle catch_ = ctxt.getCatch();
            InstanceInvocation op;
            if (catch_ == null) {
                InstanceInvocationImpl plainOp = new InstanceInvocationImpl();
                plainOp.setBasicDependency(ctxt.setDependency(plainOp));
                op = plainOp;
                plainOp.setInvocationTarget(target);
            } else {
                TryInstanceInvocationImpl tryOp = new TryInstanceInvocationImpl();
                tryOp.setBasicDependency(ctxt.setDependency(null));
                tryOp.setCatchHandler(catch_);
                // end the current block with the try/invoke, and start a new block
                NodeHandle nextBlock = new NodeHandle();
                ctxt.getAndSetCurrentBlock(nextBlock).setTarget(block(tryOp));
                tryOp.setNextBlock(nextBlock);
                tryOp.setInvocationTarget(target);
                op = tryOp;
            }
            op.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                op.setArgument(idx, argument);
            }
            op.setInstance(instance);
            op.setKind(kind);
            return op;
        }

        public Value invokeValueMethod(Context ctxt, final MethodElement target, final List<Value> arguments) {
            NodeHandle catch_ = ctxt.getCatch();
            InvocationValue value;
            if (catch_ == null) {
                InvocationValueImpl plainValue = new InvocationValueImpl();
                plainValue.setBasicDependency(ctxt.setDependency(plainValue));
                plainValue.setInvocationTarget(target);
                value = plainValue;
            } else {
                TryInvocationValueImpl tryValue = new TryInvocationValueImpl();
                tryValue.setBasicDependency(ctxt.setDependency(null));
                tryValue.setInvocationTarget(target);
                tryValue.setCatchHandler(catch_);
                // end the current block with the try/invoke, and start a new block
                NodeHandle nextBlock = new NodeHandle();
                ctxt.getAndSetCurrentBlock(nextBlock).setTarget(block(tryValue));
                tryValue.setNextBlock(nextBlock);
                value = tryValue;
            }
            value.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                value.setArgument(idx, argument);
            }
            return value;
        }

        public Value invokeInstanceValueMethod(Context ctxt, final Value instance, final InstanceInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
            NodeHandle catch_ = ctxt.getCatch();
            InstanceInvocationValue value;
            if (catch_ == null) {
                InstanceInvocationValueImpl plainValue = new InstanceInvocationValueImpl();
                plainValue.setBasicDependency(ctxt.setDependency(plainValue));
                plainValue.setInvocationTarget(target);
                value = plainValue;
            } else {
                TryInstanceInvocationValueImpl tryValue = new TryInstanceInvocationValueImpl();
                tryValue.setBasicDependency(ctxt.setDependency(null));
                tryValue.setInvocationTarget(target);
                tryValue.setCatchHandler(catch_);
                // end the current block with the try/invoke, and start a new block
                NodeHandle nextBlock = new NodeHandle();
                ctxt.getAndSetCurrentBlock(nextBlock).setTarget(block(tryValue));
                tryValue.setNextBlock(nextBlock);
                value = tryValue;
            }
            value.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                value.setArgument(idx, argument);
            }
            value.setInstance(instance);
            value.setKind(kind);
            return value;
        }

        public BasicBlock goto_(Context ctxt, final NodeHandle targetHandle) {
            GotoImpl op = new GotoImpl();
            op.setBasicDependency(ctxt.setDependency(null));
            op.setTarget(targetHandle);
            BasicBlock block = block(op);
            ctxt.getAndSetCurrentBlock(null).setTarget(block);
            return block;
        }

        public BasicBlock if_(Context ctxt, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
            IfImpl op = new IfImpl();
            op.setBasicDependency(ctxt.setDependency(null));
            op.setCondition(condition);
            op.setTrueBranch(trueTarget);
            op.setFalseBranch(falseTarget);
            BasicBlock block = block(op);
            ctxt.getAndSetCurrentBlock(null).setTarget(block);
            return block;
        }

        public BasicBlock return_(Context ctxt) {
            ReturnImpl op = new ReturnImpl();
            op.setBasicDependency(ctxt.setDependency(null));
            BasicBlock block = block(op);
            ctxt.getAndSetCurrentBlock(null).setTarget(block);
            return block;
        }

        public BasicBlock return_(Context ctxt, final Value value) {
            ValueReturnImpl op = new ValueReturnImpl();
            op.setBasicDependency(ctxt.setDependency(null));
            op.setReturnValue(value);
            BasicBlock block = block(op);
            ctxt.getAndSetCurrentBlock(null).setTarget(block);
            return block;
        }

        public BasicBlock throw_(Context ctxt, final Value value) {
            NodeHandle catch_ = ctxt.getCatch();
            ThrowImpl op;
            if (catch_ == null) {
                op = new ThrowImpl();
            } else {
                TryThrowImpl tryOp = new TryThrowImpl();
                tryOp.setCatchHandler(catch_);
                op = tryOp;
            }
            op.setBasicDependency(ctxt.setDependency(op));
            op.setThrownValue(value);
            BasicBlock block = block(op);
            ctxt.getAndSetCurrentBlock(null).setTarget(block);
            return block;
        }

        public BasicBlock jsr(final Context ctxt, final NodeHandle target, final NodeHandle ret) {
            throw new UnsupportedOperationException();
        }

        public BasicBlock ret(final Context ctxt, final Value address) {
            throw new UnsupportedOperationException();
        }

        public BasicBlock switch_(Context ctxt, final Value value, final int[] checkValues, final NodeHandle[] targets, final NodeHandle defaultTarget) {
            SwitchImpl op = new SwitchImpl();
            op.setBasicDependency(ctxt.setDependency(op));
            op.setDefaultTarget(defaultTarget);
            int length = checkValues.length;
            if (targets.length != length) {
                throw new IllegalArgumentException("Target values length does not match check values length");
            }
            for (int i = 0; i < length; i ++) {
                op.setTargetForValue(checkValues[i], targets[i]);
            }
            BasicBlock block = block(op);
            ctxt.getAndSetCurrentBlock(null).setTarget(block);
            return block;
        }

        BasicBlock block(final Terminator term) {
            BasicBlockImpl block = new BasicBlockImpl();
            block.setTerminator(term);
            return block;
        }
    };

    /**
     * Basic block building context for node dependency management, try/catch management, and any other intermediate state.
     */
    final class Context implements Cloneable {
        private Node dependency;
        private NodeHandle catch_;
        private NodeHandle currentBlock;

        public Context() {
            currentBlock = new NodeHandle();
        }

        public Context(final NodeHandle currentBlock) {
            this.currentBlock = currentBlock;
        }

        public Node getDependency() {
            return dependency;
        }

        public Node setDependency(final Node dependency) {
            try {
                return this.dependency;
            } finally {
                this.dependency = dependency;
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

        public NodeHandle getCatch() {
            return catch_;
        }

        /**
         * Set the catch block.  Setting the catch block affects whether the created node has exception
         * handling configured.
         *
         * @param newCatch the new catch block (or {@code null} for none)
         * @return the old catch block, or {@code null} if there was none
         */
        public NodeHandle setCatch(final NodeHandle newCatch) {
            try {
                return this.catch_;
            } finally {
                this.catch_ = newCatch;
            }
        }

        public NodeHandle getCurrentBlock() {
            NodeHandle currentBlock = this.currentBlock;
            if (currentBlock == null) {
                throw new IllegalStateException("No current block");
            }
            return currentBlock;
        }

        public void setCurrentBlock(final NodeHandle currentBlock) {
            this.currentBlock = currentBlock;
        }

        public NodeHandle getAndSetCurrentBlock(final NodeHandle currentBlock) {
            try {
                return getCurrentBlock();
            } finally {
                this.currentBlock = currentBlock;
            }
        }
    }
}
