package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

/**
 * A program graph factory.
 */
public interface GraphFactory {

    // delegation

    default GraphFactory getDelegate() {
        throw new IllegalStateException("No delegate");
    }

    // values

    // phi

    PhiValue phi(final Type type, BasicBlock basicBlock);

    // ternary

    Value if_(Value condition, Value trueValue, Value falseValue);

    // binary

    Value binaryOperation(CommutativeBinaryValue.Kind kind, Value v1, Value v2);

    Value binaryOperation(NonCommutativeBinaryValue.Kind kind, Value v1, Value v2);

    // unary

    Value unaryOperation(UnaryValue.Kind kind, Value v);

    // typed

    Value instanceOf(Value v, ClassType type);

    Value reinterpretCast(Value v, Type type);

    Value castOperation(WordCastValue.Kind kind, Value value, WordType toType);

    // memory

    MemoryStateValue new_(MemoryState input, ClassType type);

    MemoryStateValue newArray(MemoryState input, ArrayType type, Value size);

    MemoryStateValue pointerLoad(MemoryState input, Value pointer, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    MemoryStateValue readInstanceField(MemoryState input, Value instance, ClassType owner, String name, JavaAccessMode mode);

    MemoryStateValue readStaticField(MemoryState input, ClassType owner, String name, JavaAccessMode mode);

    MemoryStateValue readArrayValue(MemoryState input, Value array, Value index, JavaAccessMode mode);

    MemoryState pointerStore(MemoryState input, Value pointer, Value value, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    MemoryState writeInstanceField(MemoryState input, Value instance, ClassType owner, String name, Value value, JavaAccessMode mode);

    MemoryState writeStaticField(MemoryState input, ClassType owner, String name, Value value, JavaAccessMode mode);

    MemoryState writeArrayValue(MemoryState input, Value array, Value index, Value value, JavaAccessMode mode);

    MemoryState fence(MemoryState input, MemoryAtomicityMode fenceType);

    // method invocation

    MemoryState invokeMethod(MemoryState input, ClassType owner, MethodIdentifier method, List<Value> arguments);

    MemoryState invokeInstanceMethod(MemoryState input, Value instance, ClassType owner, MethodIdentifier method, List<Value> arguments);

    MemoryStateValue invokeValueMethod(MemoryState input, ClassType owner, MethodIdentifier method, List<Value> arguments);

    MemoryStateValue invokeInstanceValueMethod(MemoryState input, Value instance, ClassType owner, MethodIdentifier method, List<Value> arguments);

    // control flow

    Terminator goto_(MemoryState input, NodeHandle targetHandle);

    Terminator if_(MemoryState input, Value condition, NodeHandle trueTarget, NodeHandle falseTarget);

    Terminator return_(MemoryState input);

    Terminator return_(MemoryState input, Value value);

    Terminator throw_(MemoryState input, Value value);

    Terminator switch_(MemoryState input, Value value, int[] checkValues, NodeHandle[] targets, NodeHandle defaultTarget);

    // control flow + try

    Terminator tryInvokeMethod(MemoryState input, ClassType owner, MethodIdentifier method, List<Value> arguments, NodeHandle returnTarget, NodeHandle catchTarget);

    Terminator tryInvokeInstanceMethod(MemoryState input, Value instance, ClassType owner, MethodIdentifier method, List<Value> arguments, NodeHandle returnTarget, NodeHandle catchTarget);

    TerminatorValue tryInvokeValueMethod(MemoryState input, ClassType owner, MethodIdentifier method, List<Value> arguments, NodeHandle returnTarget, NodeHandle catchTarget);

    TerminatorValue tryInvokeInstanceValueMethod(MemoryState input, Value instance, ClassType owner, MethodIdentifier method, List<Value> arguments, NodeHandle returnTarget, NodeHandle catchTarget);

    Terminator tryThrow(MemoryState input, Value value, NodeHandle catchTarget);

    // basic block

    BasicBlock block(Terminator term);

    /**
     * A basic factory which produces each kind of node.
     */
    GraphFactory BASIC_FACTORY = new GraphFactory() {
        public Value binaryOperation(final CommutativeBinaryValue.Kind kind, final Value v1, final Value v2) {
            CommutativeBinaryValueImpl value = new CommutativeBinaryValueImpl();
            value.setKind(kind);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value binaryOperation(final NonCommutativeBinaryValue.Kind kind, final Value v1, final Value v2) {
            NonCommutativeBinaryValueImpl value = new NonCommutativeBinaryValueImpl();
            value.setKind(kind);
            value.setLeftInput(v1);
            value.setRightInput(v2);
            return value;
        }

        public Value unaryOperation(final UnaryValue.Kind kind, final Value v) {
            UnaryValueImpl value = new UnaryValueImpl();
            value.setKind(kind);
            value.setInput(v);
            return value;
        }

        public PhiValue phi(final Type type, final BasicBlock basicBlock) {
            PhiValueImpl phiValue = new PhiValueImpl(basicBlock);
            phiValue.setType(type);
            return phiValue;
        }

        public Value if_(final Value condition, final Value trueValue, final Value falseValue) {
            IfValueImpl value = new IfValueImpl();
            value.setCond(condition);
            value.setTrueValue(trueValue);
            value.setFalseValue(falseValue);
            return value;
        }

        public Value instanceOf(final Value v, final ClassType type) {
            InstanceOfValueImpl value = new InstanceOfValueImpl();
            value.setInstance(v);
            value.setInstanceType(type);
            return value;
        }

        public Value reinterpretCast(final Value v, final Type type) {
            throw new UnsupportedOperationException("Cast rework");
        }

        public Value castOperation(final WordCastValue.Kind kind, final Value value, final WordType toType) {
            WordCastValueImpl castValue = new WordCastValueImpl();
            castValue.setKind(kind);
            castValue.setInput(value);
            castValue.setType(toType);
            return castValue;
        }

        public MemoryStateValue new_(final MemoryState input, final ClassType type) {
            NewValueImpl value = new NewValueImpl();
            value.setMemoryDependency(input);
            value.setType(type);
            return value;
        }

        public MemoryStateValue newArray(final MemoryState input, final ArrayType type, final Value size) {
            NewArrayValueImpl value = new NewArrayValueImpl();
            value.setMemoryDependency(input);
            value.setType((ArrayClassType) type); // todo
            value.setSize(size);
            return value;
        }

        public MemoryStateValue pointerLoad(final MemoryState input, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
            throw new UnsupportedOperationException("Pointers");
        }

        public MemoryStateValue readInstanceField(final MemoryState input, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
            InstanceFieldReadValueImpl value = new InstanceFieldReadValueImpl();
            value.setMemoryDependency(input);
            value.setInstance(instance);
            value.setFieldOwner(owner);
            value.setFieldName(name);
            value.setMode(mode);
            return value;
        }

        public MemoryStateValue readStaticField(final MemoryState input, final ClassType owner, final String name, final JavaAccessMode mode) {
            StaticFieldReadValueImpl value = new StaticFieldReadValueImpl();
            value.setMemoryDependency(input);
            value.setFieldOwner(owner);
            value.setFieldName(name);
            value.setMode(mode);
            return value;
        }

        public MemoryStateValue readArrayValue(final MemoryState input, final Value array, final Value index, final JavaAccessMode mode) {
            ArrayElementReadValueImpl value = new ArrayElementReadValueImpl();
            value.setMemoryDependency(input);
            value.setInstance(array);
            value.setIndex(index);
            value.setMode(mode);
            return value;
        }

        public MemoryState pointerStore(final MemoryState input, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
            throw new UnsupportedOperationException("Pointers");
        }

        public MemoryState writeInstanceField(final MemoryState input, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
            InstanceFieldWriteImpl op = new InstanceFieldWriteImpl();
            op.setMemoryDependency(input);
            op.setInstance(instance);
            op.setFieldOwner(owner);
            op.setFieldName(name);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public MemoryState writeStaticField(final MemoryState input, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
            StaticFieldWriteImpl op = new StaticFieldWriteImpl();
            op.setMemoryDependency(input);
            op.setFieldOwner(owner);
            op.setFieldName(name);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public MemoryState writeArrayValue(final MemoryState input, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
            ArrayElementWriteImpl op = new ArrayElementWriteImpl();
            op.setMemoryDependency(input);
            op.setInstance(array);
            op.setIndex(index);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public MemoryState fence(final MemoryState input, final MemoryAtomicityMode fenceType) {
            throw new UnsupportedOperationException("Fence");
        }

        public MemoryState invokeMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
            InvocationImpl op = new InvocationImpl();
            op.setMemoryDependency(input);
            op.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                op.setArgument(idx, argument);
            }
            op.setMethodOwner(owner);
            op.setInvocationTarget(method);
            return op;
        }

        public MemoryState invokeInstanceMethod(final MemoryState input, final Value instance, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
            InstanceInvocationImpl op = new InstanceInvocationImpl();
            op.setMemoryDependency(input);
            op.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                op.setArgument(idx, argument);
            }
            op.setMethodOwner(owner);
            op.setInvocationTarget(method);
            op.setInstance(instance);
            return op;
        }

        public MemoryStateValue invokeValueMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
            InvocationValueImpl value = new InvocationValueImpl();
            value.setMemoryDependency(input);
            value.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                value.setArgument(idx, argument);
            }
            value.setMethodOwner(owner);
            value.setInvocationTarget(method);
            return value;
        }

        public MemoryStateValue invokeInstanceValueMethod(final MemoryState input, final Value instance, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
            InstanceInvocationValueImpl value = new InstanceInvocationValueImpl();
            value.setMemoryDependency(input);
            value.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                value.setArgument(idx, argument);
            }
            value.setMethodOwner(owner);
            value.setInvocationTarget(method);
            value.setInstance(instance);
            return value;
        }

        public Terminator goto_(final MemoryState input, final NodeHandle targetHandle) {
            GotoImpl op = new GotoImpl();
            op.setMemoryDependency(input);
            op.setNextBlock(targetHandle);
            return op;
        }

        public Terminator if_(final MemoryState input, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
            IfImpl op = new IfImpl();
            op.setMemoryDependency(input);
            op.setCondition(condition);
            op.setTrueBranch(trueTarget);
            op.setFalseBranch(falseTarget);
            return op;
        }

        public Terminator return_(final MemoryState input) {
            ReturnImpl op = new ReturnImpl();
            op.setMemoryDependency(input);
            return op;
        }

        public Terminator return_(final MemoryState input, final Value value) {
            ValueReturnImpl op = new ValueReturnImpl();
            op.setMemoryDependency(input);
            op.setReturnValue(value);
            return op;
        }

        public Terminator throw_(final MemoryState input, final Value value) {
            ThrowImpl op = new ThrowImpl();
            op.setMemoryDependency(input);
            op.setThrownValue(value);
            return op;
        }

        public Terminator switch_(final MemoryState input, final Value value, final int[] checkValues, final NodeHandle[] targets, final NodeHandle defaultTarget) {
            SwitchImpl op = new SwitchImpl();
            op.setMemoryDependency(input);
            op.setDefaultTarget(defaultTarget);
            int length = checkValues.length;
            if (targets.length != length) {
                throw new IllegalArgumentException("Target values length does not match check values length");
            }
            for (int i = 0; i < length; i ++) {
                op.setTargetForValue(checkValues[i], targets[i]);
            }
            return op;
        }

        public Terminator tryInvokeMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
            TryInvocationImpl op = new TryInvocationImpl();
            op.setMemoryDependency(input);
            op.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                op.setArgument(idx, argument);
            }
            op.setMethodOwner(owner);
            op.setInvocationTarget(method);
            op.setCatchHandler(catchTarget);
            op.setNextBlock(returnTarget);
            return op;
        }

        public Terminator tryInvokeInstanceMethod(final MemoryState input, final Value instance, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
            TryInstanceInvocationImpl op = new TryInstanceInvocationImpl();
            op.setMemoryDependency(input);
            op.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                op.setArgument(idx, argument);
            }
            op.setMethodOwner(owner);
            op.setInvocationTarget(method);
            op.setInstance(instance);
            op.setCatchHandler(catchTarget);
            op.setNextBlock(returnTarget);
            return op;
        }

        public TerminatorValue tryInvokeValueMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
            TryInvocationValueImpl value = new TryInvocationValueImpl();
            value.setMemoryDependency(input);
            value.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                value.setArgument(idx, argument);
            }
            value.setMethodOwner(owner);
            value.setInvocationTarget(method);
            value.setCatchHandler(catchTarget);
            value.setNextBlock(returnTarget);
            return value;
        }

        public TerminatorValue tryInvokeInstanceValueMethod(final MemoryState input, final Value instance, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
            TryInstanceInvocationValueImpl value = new TryInstanceInvocationValueImpl();
            value.setMemoryDependency(input);
            value.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                value.setArgument(idx, argument);
            }
            value.setMethodOwner(owner);
            value.setInvocationTarget(method);
            value.setInstance(instance);
            value.setCatchHandler(catchTarget);
            value.setNextBlock(returnTarget);
            return value;
        }

        public Terminator tryThrow(final MemoryState input, final Value value, final NodeHandle catchTarget) {
            TryThrowImpl op = new TryThrowImpl();
            op.setMemoryDependency(input);
            op.setThrownValue(value);
            op.setCatchHandler(catchTarget);
            return op;
        }

        public BasicBlock block(final Terminator term) {
            BasicBlockImpl block = new BasicBlockImpl();
            block.setTerminator(term);
            return block;
        }
    };

    interface MemoryStateValue {
        Value getValue();
        MemoryState getMemoryState();
        static MemoryStateValue create(Value value, MemoryState state) {
            return new MemoryStateValue() {
                public Value getValue() {
                    return value;
                }

                public MemoryState getMemoryState() {
                    return state;
                }
            };
        }
    }

    interface TerminatorValue {
        Value getValue();
        Terminator getTerminator();
    }
}
