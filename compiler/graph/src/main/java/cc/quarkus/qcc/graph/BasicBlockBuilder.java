package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.graph.literal.ArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeIdType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;

/**
 * A program graph builder, which builds each basic block in succession and wires them together.
 */
public interface BasicBlockBuilder {

    // values

    Value receiver(TypeIdLiteral upperBound);

    Value parameter(ValueType type, int index);

    Value catch_(TypeIdLiteral upperBound);

    // phi

    PhiValue phi(ValueType type);

    // ternary

    Value select(Value condition, Value trueValue, Value falseValue);

    // binary

    Value add(Value v1, Value v2);

    Value multiply(Value v1, Value v2);

    Value and(Value v1, Value v2);

    Value or(Value v1, Value v2);

    Value xor(Value v1, Value v2); // also used for ones-complement

    Value cmpEq(Value v1, Value v2);

    Value cmpNe(Value v1, Value v2);

    Value shr(Value v1, Value v2);

    Value shl(Value v1, Value v2);

    Value sub(Value v1, Value v2); // also used for twos-complement

    Value divide(Value v1, Value v2);

    Value remainder(Value v1, Value v2);

    Value cmpLt(Value v1, Value v2);

    Value cmpGt(Value v1, Value v2);

    Value cmpLe(Value v1, Value v2);

    Value cmpGe(Value v1, Value v2);

    Value rol(Value v1, Value v2);

    Value ror(Value v1, Value v2);

    // unary

    Value negate(Value v); // neg is only needed for FP; ints should use 0-n

    Value byteSwap(Value v);

    Value bitReverse(Value v);

    Value countLeadingZeros(Value v);

    Value countTrailingZeros(Value v);

    Value populationCount(Value v);

    Value arrayLength(Value array);

    // typed

    /**
     * Get the type ID of the given reference value.
     *
     * @param value the value, whose type must be a {@link ReferenceType}
     * @return the type ID, whose type must be a {@link TypeIdType}
     */
    Value typeIdOf(Value value);

    Value truncate(Value value, WordType toType);

    Value extend(Value value, WordType toType);

    Value bitCast(Value value, WordType toType);

    Value valueConvert(Value value, WordType toType);

    /**
     * Narrow a value with reference type to another (typically more specific) type.
     *
     * @param value the value to narrow
     * @param toType the type to narrow to
     * @return the narrowed type
     */
    Value narrow(Value value, TypeIdLiteral toType);

    // memory

    Value new_(ClassTypeIdLiteral typeId);

    Value newArray(ArrayTypeIdLiteral arrayTypeId, Value size);

    Value multiNewArray(ArrayTypeIdLiteral arrayTypeId, Value... dimensions);

    Value clone(Value object);

    Value pointerLoad(Value pointer, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    Value readInstanceField(Value instance, FieldElement fieldElement, JavaAccessMode mode);

    Value readStaticField(FieldElement fieldElement, JavaAccessMode mode);

    Value readArrayValue(Value array, Value index, JavaAccessMode mode);

    Node pointerStore(Value pointer, Value value, MemoryAccessMode accessMode, MemoryAtomicityMode atomicityMode);

    Node writeInstanceField(Value instance, FieldElement fieldElement, Value value, JavaAccessMode mode);

    Node writeStaticField(FieldElement fieldElement, Value value, JavaAccessMode mode);

    Node writeArrayValue(Value array, Value index, Value value, JavaAccessMode mode);

    Node fence(MemoryAtomicityMode fenceType);

    Node monitorEnter(Value obj);

    Node monitorExit(Value obj);

    // method invocation

    Node invokeStatic(MethodElement target, List<Value> arguments);

    Node invokeInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments);

    Value invokeValueStatic(MethodElement target, List<Value> arguments);

    Value invokeInstanceValueMethod(Value instance, DispatchInvocation.Kind kind, MethodElement target, List<Value> arguments);

    /**
     * Invoke an object instance initializer.  The value returned has an initialized type.  The returned value should
     * replace all occurrences of the uninitialized value when processing bytecode.
     *
     * @param instance the uninitialized instance to initialize (must not be {@code null})
     * @param target the constructor to invoke (must not be {@code null})
     * @param arguments the constructor arguments (must not be {@code null})
     * @return the initialized value
     */
    Value invokeConstructor(Value instance, ConstructorElement target, List<Value> arguments);

    // misc

    /**
     * No operation.  The returned node is not guaranteed to be unique or of any particular type.
     *
     * @return a node that does not change the behavior of the block
     */
    Node nop();

    /**
     * Begin a new block.  The returned node will be a dependency (usually the topmost dependency) of the terminator.
     *
     * @param blockLabel the label of the new block (must not be {@code null} or resolved)
     * @return the node representing the block entry
     */
    Node begin(BlockLabel blockLabel);

    // control flow - terminalBlock is updated to point to this terminator

    /**
     * Generate a {@code goto} termination node.  The terminated block is returned.
     *
     * @param resumeLabel the handle of the jump target (must not be {@code null})
     * @return the terminated block
     */
    BasicBlock goto_(BlockLabel resumeLabel);

    /**
     * Construct an {@code if} node.  If the condition is true, the {@code trueTarget} will receive control.  Otherwise,
     * the {@code falseTarget} will receive control.
     * <p>
     * Terminates the current block, which is returned.
     *
     * @param condition the condition (must not be {@code null})
     * @param trueTarget the execution target to use when {@code condition} is {@code true}
     * @param falseTarget the execution target to use when {@code condition} is {@code false}
     * @return the terminated block
     */
    BasicBlock if_(Value condition, BlockLabel trueTarget, BlockLabel falseTarget);

    BasicBlock return_();

    BasicBlock return_(Value value);

    BasicBlock throw_(Value value);

    BasicBlock switch_(Value value, int[] checkValues, BlockLabel[] targets, BlockLabel defaultTarget);

    /**
     * Construct a {@code jsr} node which must be returned from.  Before lowering, {@code jsr} nodes are inlined,
     * copying all of the nodes into new basic blocks.
     * <p>
     * Terminates the current block.
     *
     * @param subLabel the subroutine call target (must not be {@code null})
     * @param returnAddress the return address literal (must not be {@code null})
     * @return the terminated block
     */
    BasicBlock jsr(BlockLabel subLabel, BlockLiteral returnAddress);

    /**
     * Return from a {@code jsr} subroutine call.
     * <p>
     * Terminates the current block.
     *
     * @param address the return address (must not be {@code null})
     * @return the node
     */
    BasicBlock ret(Value address);

    BasicBlock try_(Triable operation, ClassTypeIdLiteral[] catchTypeIds, BlockLabel[] catchTargetLabels, BlockLabel resumeLabel);

    /**
     * Terminate the block with a class cast exception.
     *
     * @param fromType the type ID of the value being cast
     * @param toType the target type ID
     * @return the terminated block
     */
    BasicBlock classCastException(Value fromType, Value toType);

    interface Factory {
        BasicBlockBuilder construct(Context context, BasicBlockBuilder delegate);

        interface Context {
            TypeSystem getTypeSystem();

            LiteralFactory getLiteralFactory();
        }
    }

    static BasicBlockBuilder simpleBuilder(final TypeSystem typeSystem) {
        return new BasicBlockBuilder() {
            private Node dependency;
            private BlockLabel currentBlock;

            public Value add(final Value v1, final Value v2) {
                return new Add(v1, v2);
            }

            public Value multiply(final Value v1, final Value v2) {
                return new Multiply(v1, v2);
            }

            public Value and(final Value v1, final Value v2) {
                return new And(v1, v2);
            }

            public Value or(final Value v1, final Value v2) {
                return new Or(v1, v2);
            }

            public Value xor(final Value v1, final Value v2) {
                return new Xor(v1, v2);
            }

            public Value cmpEq(final Value v1, final Value v2) {
                return new CmpEq(v1, v2, typeSystem.getBooleanType());
            }

            public Value cmpNe(final Value v1, final Value v2) {
                return new CmpNe(v1, v2, typeSystem.getBooleanType());
            }

            public Value shr(final Value v1, final Value v2) {
                return new Shr(v1, v2);
            }

            public Value shl(final Value v1, final Value v2) {
                return new Shl(v1, v2);
            }

            public Value sub(final Value v1, final Value v2) {
                return new Sub(v1, v2);
            }

            public Value divide(final Value v1, final Value v2) {
                return new Div(v1, v2);
            }

            public Value remainder(final Value v1, final Value v2) {
                return new Mod(v1, v2);
            }

            public Value cmpLt(final Value v1, final Value v2) {
                return new CmpLt(v1, v2, typeSystem.getBooleanType());
            }

            public Value cmpGt(final Value v1, final Value v2) {
                return new CmpGt(v1, v2, typeSystem.getBooleanType());
            }

            public Value cmpLe(final Value v1, final Value v2) {
                return new CmpLe(v1, v2, typeSystem.getBooleanType());
            }

            public Value cmpGe(final Value v1, final Value v2) {
                return new CmpGe(v1, v2, typeSystem.getBooleanType());
            }

            public Value rol(final Value v1, final Value v2) {
                return new Rol(v1, v2);
            }

            public Value ror(final Value v1, final Value v2) {
                return new Ror(v1, v2);
            }

            public Value negate(final Value v) {
                return new Neg(v);
            }

            public Value byteSwap(final Value v) {
                throw Assert.unsupported();
            }

            public Value bitReverse(final Value v) {
                throw Assert.unsupported();
            }

            public Value countLeadingZeros(final Value v) {
                throw Assert.unsupported();
            }

            public Value countTrailingZeros(final Value v) {
                throw Assert.unsupported();
            }

            public Value populationCount(final Value v) {
                throw Assert.unsupported();
            }

            public Value arrayLength(final Value array) {
                return new ArrayLength(array, typeSystem.getSignedInteger32Type());
            }

            public Value truncate(final Value value, final WordType toType) {
                return new Truncate(value, toType);
            }

            public Value extend(final Value value, final WordType toType) {
                return new Extend(value, toType);
            }

            public Value bitCast(final Value value, final WordType toType) {
                return new BitCast(value, toType);
            }

            public Value valueConvert(final Value value, final WordType toType) {
                return new Convert(value, toType);
            }

            public Value narrow(final Value value, final TypeIdLiteral toType) {
                return new Narrow(value, typeSystem.getReferenceType(toType));
            }

            public Value receiver(final TypeIdLiteral upperBound) {
                return new ThisValue(typeSystem.getReferenceType(Assert.checkNotNullParam("upperBound", upperBound)));
            }

            public Value parameter(final ValueType type, final int index) {
                return new ParameterValue(type, index);
            }

            public Value catch_(final TypeIdLiteral upperBound) {
                return new Catch(requireCurrentBlock(), typeSystem.getReferenceType(Assert.checkNotNullParam("upperBound", upperBound)));
            }

            public PhiValue phi(final ValueType type) {
                return new PhiValue(type, requireCurrentBlock());
            }

            public Value select(final Value condition, final Value trueValue, final Value falseValue) {
                return new Select(condition, trueValue, falseValue);
            }

            public Value typeIdOf(final Value value) {
                return new TypeIdOf(typeSystem.getTypeIdType(), value);
            }

            public Value new_(final ClassTypeIdLiteral typeId) {
                return asDependency(new New(requireDependency(), typeSystem.getReferenceType(typeId), typeId));
            }

            public Value newArray(final ArrayTypeIdLiteral arrayTypeId, final Value size) {
                return asDependency(new NewArray(requireDependency(), arrayTypeId, typeSystem.getReferenceType(arrayTypeId), size));
            }

            public Value multiNewArray(final ArrayTypeIdLiteral arrayTypeId, final Value... dimensions) {
                throw Assert.unsupported();
            }

            public Value clone(final Value object) {
                throw Assert.unsupported();
            }

            public Value pointerLoad(final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
                throw Assert.unsupported();
            }

            public Value readInstanceField(final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
                return asDependency(new InstanceFieldRead(requireDependency(), instance, fieldElement, mode));
            }

            public Value readStaticField(final FieldElement fieldElement, final JavaAccessMode mode) {
                return asDependency(new StaticFieldRead(requireDependency(), fieldElement, mode));
            }

            public Value readArrayValue(final Value array, final Value index, final JavaAccessMode mode) {
                return asDependency(new ArrayElementRead(requireDependency(), array, index, mode));
            }

            public Node pointerStore(final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
                throw Assert.unsupported();
            }

            public Node writeInstanceField(final Value instance, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
                return asDependency(new InstanceFieldWrite(requireDependency(), instance, fieldElement, value, mode));
            }

            public Node writeStaticField(final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
                return asDependency(new StaticFieldWrite(requireDependency(), fieldElement, value, mode));
            }

            public Node writeArrayValue(final Value array, final Value index, final Value value, final JavaAccessMode mode) {
                return asDependency(new ArrayElementWrite(requireDependency(), array, index, value, mode));
            }

            public Node fence(final MemoryAtomicityMode fenceType) {
                throw Assert.unsupported();
            }

            public Node monitorEnter(final Value obj) {
                throw Assert.unsupported();
            }

            public Node monitorExit(final Value obj) {
                throw Assert.unsupported();
            }

            public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
                return asDependency(new StaticInvocation(requireDependency(), target, arguments));
            }

            public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
                return asDependency(new InstanceInvocation(requireDependency(), kind, instance, target, arguments));
            }

            public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
                return asDependency(new StaticInvocationValue(requireDependency(), target, arguments));
            }

            public Value invokeInstanceValueMethod(final Value instance, final DispatchInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
                return asDependency(new InstanceInvocationValue(requireDependency(), kind, instance, target, arguments));
            }

            public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
                return asDependency(new ConstructorInvocation(requireDependency(), instance, target, arguments));
            }

            public Node nop() {
                return requireDependency();
            }

            private <N extends Node> N asDependency(N node) {
                this.dependency = node;
                return node;
            }

            public Node begin(final BlockLabel blockLabel) {
                Assert.checkNotNullParam("blockLabel", blockLabel);
                if (blockLabel.hasTarget()) {
                    throw new IllegalStateException("Block already terminated");
                }
                if (currentBlock != null) {
                    throw new IllegalStateException("Block already in progress");
                }
                currentBlock = blockLabel;
                return dependency = new BlockEntry(blockLabel);
            }

            public BasicBlock goto_(final BlockLabel resumeLabel) {
                return terminate(requireCurrentBlock(), new Goto(dependency, resumeLabel));
            }

            public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
                return terminate(requireCurrentBlock(), new If(dependency, condition, trueTarget, falseTarget));
            }

            public BasicBlock return_() {
                return terminate(requireCurrentBlock(), new Return(dependency));
            }

            public BasicBlock return_(final Value value) {
                return terminate(requireCurrentBlock(), new ValueReturn(dependency, value));
            }

            public BasicBlock throw_(final Value value) {
                return terminate(requireCurrentBlock(), new Throw(dependency, value));
            }

            public BasicBlock jsr(final BlockLabel subLabel, final BlockLiteral returnAddress) {
                return terminate(requireCurrentBlock(), new Jsr(dependency, subLabel, returnAddress));
            }

            public BasicBlock ret(final Value address) {
                return terminate(requireCurrentBlock(), new Ret(dependency, address));
            }

            public BasicBlock try_(final Triable operation, final ClassTypeIdLiteral[] catchTypeIds, final BlockLabel[] catchTargetLabels, final BlockLabel resumeLabel) {
                return terminate(requireCurrentBlock(), new Try(dependency, operation, catchTypeIds, catchTargetLabels, resumeLabel));
            }

            public BasicBlock classCastException(final Value fromType, final Value toType) {
                return terminate(requireCurrentBlock(), new ClassCastErrorNode(dependency, fromType, toType));
            }

            public BasicBlock switch_(final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget) {
                return terminate(requireCurrentBlock(), new Switch(dependency, defaultTarget, checkValues, targets, value));
            }

            private BasicBlock terminate(final BlockLabel block, final Terminator op) {
                BasicBlock realBlock = new BasicBlock(op);
                block.setTarget(realBlock);
                currentBlock = null;
                dependency = null;
                return realBlock;
            }

            private BlockLabel requireCurrentBlock() {
                BlockLabel block = this.currentBlock;
                if (block == null) {
                    assert dependency == null;
                    throw noBlock();
                }
                assert dependency != null;
                return block;
            }

            private Node requireDependency() {
                Node dependency = this.dependency;
                if (dependency == null) {
                    assert currentBlock == null;
                    throw noBlock();
                }
                assert currentBlock != null;
                return dependency;
            }

            private IllegalStateException noBlock() {
                return new IllegalStateException("No block in progress");
            }
        };
    }
}
