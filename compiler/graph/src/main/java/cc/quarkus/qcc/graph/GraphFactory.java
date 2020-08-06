package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

/**
 * A program graph factory.
 */
public interface GraphFactory {

    // values

    // phi

    PhiValue phi(Type type, BasicBlock basicBlock);

    // ternary

    Value if_(Value condition, Value trueValue, Value falseValue);

    // binary

    Value binaryOperation(CommutativeBinaryValue.Kind kind, Value v1, Value v2);

    Value binaryOperation(NonCommutativeBinaryValue.Kind kind, Value v1, Value v2);

    // unary

    Value unaryOperation(UnaryValue.Kind kind, Value v);

    Value lengthOfArray(Value array);

    // typed

    Value instanceOf(Value v, ClassType type);

    Value reinterpretCast(Value v, Type type);

    Value castOperation(WordCastValue.Kind kind, Value value, WordType toType);

    // memory/dependency

    PhiDependency phiDependency(BasicBlock basicBlock);

    Node multiDependency(Node... nodes);

    Value multiDependency(Value value, Node... nodes);

    Value new_(Node dependency, ClassType type);

    Value newArray(Node dependency, ArrayType type, Value size);

    Value pointerLoad(Node dependency, Value pointer, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    Value readInstanceField(Node dependency, Value instance, ClassType owner, String name, JavaAccessMode mode);

    Value readStaticField(Node dependency, ClassType owner, String name, JavaAccessMode mode);

    Value readArrayValue(Node dependency, Value array, Value index, JavaAccessMode mode);

    Node pointerStore(Node dependency, Value pointer, Value value, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    Node writeInstanceField(Node dependency, Value instance, ClassType owner, String name, Value value, JavaAccessMode mode);

    Node writeStaticField(Node dependency, ClassType owner, String name, Value value, JavaAccessMode mode);

    Node writeArrayValue(Node dependency, Value array, Value index, Value value, JavaAccessMode mode);

    Node fence(Node dependency, MemoryAtomicityMode fenceType);

    // method invocation

    Node invokeMethod(Node dependency, ClassType owner, MethodIdentifier method, List<Value> arguments);

    Node invokeInstanceMethod(Node dependency, Value instance, InstanceInvocation.Kind kind, ClassType owner, MethodIdentifier method, List<Value> arguments);

    Value invokeValueMethod(Node dependency, ClassType owner, MethodIdentifier method, List<Value> arguments);

    Value invokeInstanceValueMethod(Node dependency, Value instance, InstanceInvocation.Kind kind, ClassType owner, MethodIdentifier method, List<Value> arguments);

    // control flow

    Terminator goto_(Node dependency, NodeHandle targetHandle);

    Terminator if_(Node dependency, Value condition, NodeHandle trueTarget, NodeHandle falseTarget);

    Terminator return_(Node dependency);

    Terminator return_(Node dependency, Value value);

    Terminator throw_(Node dependency, Value value);

    Terminator switch_(Node dependency, Value value, int[] checkValues, NodeHandle[] targets, NodeHandle defaultTarget);

    // control flow + try

    Terminator tryInvokeMethod(Node dependency, ClassType owner, MethodIdentifier method, List<Value> arguments, NodeHandle returnTarget, NodeHandle catchTarget);

    Terminator tryInvokeInstanceMethod(Node dependency, Value instance, InstanceInvocation.Kind kind, ClassType owner, MethodIdentifier method, List<Value> arguments, NodeHandle returnTarget, NodeHandle catchTarget);

    ValueTerminator tryInvokeValueMethod(Node dependency, ClassType owner, MethodIdentifier method, List<Value> arguments, NodeHandle returnTarget, NodeHandle catchTarget);

    ValueTerminator tryInvokeInstanceValueMethod(Node dependency, Value instance, InstanceInvocation.Kind kind, ClassType owner, MethodIdentifier method, List<Value> arguments, NodeHandle returnTarget, NodeHandle catchTarget);

    Terminator tryThrow(Node dependency, Value value, NodeHandle catchTarget);

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

        public Value lengthOfArray(final Value array) {
            ArrayLengthValueImpl value = new ArrayLengthValueImpl();
            value.setInstance(array);
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

        public PhiDependency phiDependency(final BasicBlock basicBlock) {
            return new PhiDependencyImpl(basicBlock);
        }

        public Node multiDependency(final Node... nodes) {
            throw new UnsupportedOperationException("Multi dependency");
        }

        public Value multiDependency(final Value value, final Node... nodes) {
            throw new UnsupportedOperationException("Multi dependency");
        }

        public Value new_(final Node dependency, final ClassType type) {
            NewValueImpl value = new NewValueImpl();
            value.setBasicDependency(dependency);
            value.setType(type);
            return value;
        }

        public Value newArray(final Node dependency, final ArrayType type, final Value size) {
            NewArrayValueImpl value = new NewArrayValueImpl();
            value.setBasicDependency(dependency);
            value.setType((ArrayClassType) type); // todo
            value.setSize(size);
            return value;
        }

        public Value pointerLoad(final Node dependency, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
            throw new UnsupportedOperationException("Pointers");
        }

        public Value readInstanceField(final Node dependency, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
            InstanceFieldReadValueImpl value = new InstanceFieldReadValueImpl();
            value.setBasicDependency(dependency);
            value.setInstance(instance);
            value.setFieldOwner(owner);
            value.setFieldName(name);
            value.setMode(mode);
            return value;
        }

        public Value readStaticField(final Node dependency, final ClassType owner, final String name, final JavaAccessMode mode) {
            StaticFieldReadValueImpl value = new StaticFieldReadValueImpl();
            value.setBasicDependency(dependency);
            value.setFieldOwner(owner);
            value.setFieldName(name);
            value.setMode(mode);
            return value;
        }

        public Value readArrayValue(final Node dependency, final Value array, final Value index, final JavaAccessMode mode) {
            ArrayElementReadValueImpl value = new ArrayElementReadValueImpl();
            value.setBasicDependency(dependency);
            value.setInstance(array);
            value.setIndex(index);
            value.setMode(mode);
            return value;
        }

        public Node pointerStore(final Node dependency, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
            throw new UnsupportedOperationException("Pointers");
        }

        public Node writeInstanceField(final Node dependency, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
            InstanceFieldWriteImpl op = new InstanceFieldWriteImpl();
            op.setBasicDependency(dependency);
            op.setInstance(instance);
            op.setFieldOwner(owner);
            op.setFieldName(name);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public Node writeStaticField(final Node dependency, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
            StaticFieldWriteImpl op = new StaticFieldWriteImpl();
            op.setBasicDependency(dependency);
            op.setFieldOwner(owner);
            op.setFieldName(name);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public Node writeArrayValue(final Node dependency, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
            ArrayElementWriteImpl op = new ArrayElementWriteImpl();
            op.setBasicDependency(dependency);
            op.setInstance(array);
            op.setIndex(index);
            op.setWriteValue(value);
            op.setMode(mode);
            return op;
        }

        public Node fence(final Node dependency, final MemoryAtomicityMode fenceType) {
            throw new UnsupportedOperationException("Fence");
        }

        public Node invokeMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
            InvocationImpl op = new InvocationImpl();
            op.setBasicDependency(dependency);
            op.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                op.setArgument(idx, argument);
            }
            op.setMethodOwner(owner);
            op.setInvocationTarget(method);
            return op;
        }

        public Node invokeInstanceMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
            InstanceInvocationImpl op = new InstanceInvocationImpl();
            op.setBasicDependency(dependency);
            op.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                op.setArgument(idx, argument);
            }
            op.setMethodOwner(owner);
            op.setInvocationTarget(method);
            op.setInstance(instance);
            op.setKind(kind);
            return op;
        }

        public Value invokeValueMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
            InvocationValueImpl value = new InvocationValueImpl();
            value.setBasicDependency(dependency);
            value.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                value.setArgument(idx, argument);
            }
            value.setMethodOwner(owner);
            value.setInvocationTarget(method);
            return value;
        }

        public Value invokeInstanceValueMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
            InstanceInvocationValueImpl value = new InstanceInvocationValueImpl();
            value.setBasicDependency(dependency);
            value.setArgumentCount(arguments.size());
            int idx = 0;
            for (Value argument : arguments) {
                value.setArgument(idx, argument);
            }
            value.setMethodOwner(owner);
            value.setInvocationTarget(method);
            value.setInstance(instance);
            value.setKind(kind);
            return value;
        }

        public Terminator goto_(final Node dependency, final NodeHandle targetHandle) {
            GotoImpl op = new GotoImpl();
            op.setBasicDependency(dependency);
            op.setNextBlock(targetHandle);
            return op;
        }

        public Terminator if_(final Node dependency, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
            IfImpl op = new IfImpl();
            op.setBasicDependency(dependency);
            op.setCondition(condition);
            op.setTrueBranch(trueTarget);
            op.setFalseBranch(falseTarget);
            return op;
        }

        public Terminator return_(final Node dependency) {
            ReturnImpl op = new ReturnImpl();
            op.setBasicDependency(dependency);
            return op;
        }

        public Terminator return_(final Node dependency, final Value value) {
            ValueReturnImpl op = new ValueReturnImpl();
            op.setBasicDependency(dependency);
            op.setReturnValue(value);
            return op;
        }

        public Terminator throw_(final Node dependency, final Value value) {
            ThrowImpl op = new ThrowImpl();
            op.setBasicDependency(dependency);
            op.setThrownValue(value);
            return op;
        }

        public Terminator switch_(final Node dependency, final Value value, final int[] checkValues, final NodeHandle[] targets, final NodeHandle defaultTarget) {
            SwitchImpl op = new SwitchImpl();
            op.setBasicDependency(dependency);
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

        public Terminator tryInvokeMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
            TryInvocationImpl op = new TryInvocationImpl();
            op.setBasicDependency(dependency);
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

        public Terminator tryInvokeInstanceMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
            TryInstanceInvocationImpl op = new TryInstanceInvocationImpl();
            op.setBasicDependency(dependency);
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
            op.setKind(kind);
            return op;
        }

        public ValueTerminator tryInvokeValueMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
            TryInvocationValueImpl value = new TryInvocationValueImpl();
            value.setBasicDependency(dependency);
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

        public ValueTerminator tryInvokeInstanceValueMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
            TryInstanceInvocationValueImpl value = new TryInstanceInvocationValueImpl();
            value.setBasicDependency(dependency);
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
            value.setKind(kind);
            return value;
        }

        public Terminator tryThrow(final Node dependency, final Value value, final NodeHandle catchTarget) {
            TryThrowImpl op = new TryThrowImpl();
            op.setBasicDependency(dependency);
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
}
