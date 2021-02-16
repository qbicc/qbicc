package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.TypeType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.GlobalVariableElement;
import cc.quarkus.qcc.type.definition.element.LocalVariableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 * A program graph builder, which builds each basic block in succession and wires them together.
 */
public interface BasicBlockBuilder {
    // context

    /**
     * Get the first builder in this chain.
     *
     * @return the first builder ((must not be {@code null}))
     */
    BasicBlockBuilder getFirstBuilder();

    /**
     * Set the first builder in this chain.
     *
     * @param first the first builder (must not be {@code null})
     */
    void setFirstBuilder(BasicBlockBuilder first);

    /**
     * Get the element currently being built.
     *
     * @return the element currently being built
     */
    ExecutableElement getCurrentElement();

    /**
     * Set the element currently being built, returning the previously-set element.  Used for inlining.
     *
     * @param element the new current element
     * @return the previously set current element
     */
    ExecutableElement setCurrentElement(ExecutableElement element);

    /**
     * Get the currently set call site node.  Used for inlining.
     *
     * @return the currently set call site node
     */
    Node getCallSite();

    /**
     * Set the call site node.  Used for inlining.
     *
     * @param callSite the call site node
     * @return the previously set call site node
     */
    Node setCallSite(Node callSite);

    /**
     * Get a location for the element currently being built, suitable for passing to diagnostics.
     *
     * @return the location
     */
    Location getLocation();

    /**
     * Set the line number to use for subsequently built nodes.  Use {@code 0} for no line number.
     *
     * @param newLineNumber the line number
     * @return the previously set line number
     */
    int setLineNumber(int newLineNumber);

    /**
     * Set the bytecode index to use for subsequently built nodes.  Use {@code -1} for no bytecode index.
     *
     * @param newBytecodeIndex the bytecode index
     * @return the previously set bytecode index
     */
    int setBytecodeIndex(int newBytecodeIndex);

    /**
     * Get or compute the currently active exception handler.  Returns {@code null} if no exception handler is
     * active (i.e. exceptions should be propagated to the caller).
     * <p>
     * This method generally should not be overridden (overriding this method does not change the exception handler
     * selected in most cases because the exception handler is selected by the last builder in the chain).  Its purpose
     * is to provide a means for builders to locate the exception handler for their own purposes.
     *
     * @return the currently active exception handler, or {@code null} if exceptions should propagate to the caller
     */
    ExceptionHandler getExceptionHandler();

    /**
     * Set the exception handler policy.  The set policy will determine the exception handler that is returned from
     * {@link #getExceptionHandler()}.
     *
     * @param policy the exception handler policy (must not be {@code null})
     */
    void setExceptionHandlerPolicy(ExceptionHandlerPolicy policy);

    /**
     * Indicate that all construction is complete.
     */
    void finish();

    // values

    ParameterValue parameter(ValueType type, String label, int index);

    Value currentThread();

    // phi

    PhiValue phi(ValueType type, BlockLabel owner);

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

    Value min(Value v1, Value v2);

    Value max(Value v1, Value v2);

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

    Value arrayLength(ValueHandle arrayHandle);

    // typed

    /**
     * Get the type ID of the given reference value.
     *
     * @param valueHandle the value, whose type must be a {@link ReferenceType}
     * @return the type ID, whose type must be a {@link TypeType}
     */
    Value typeIdOf(ValueHandle valueHandle);

    /**
     * Get the {@link Class} object for the given type ID value, whose type must be a {@link TypeType} with
     * an upper bound which is a {@link ObjectType}.
     *
     * @param typeId the type ID value
     * @return a non-nullable narrowed reference to the class object for the given type ID value
     */
    Value classOf(Value typeId);

    Value truncate(Value value, WordType toType);

    Value extend(Value value, WordType toType);

    Value bitCast(Value value, WordType toType);

    Value valueConvert(Value value, WordType toType);

    Value instanceOf(Value input, ValueType expectedType);

    Value instanceOf(Value input, TypeDescriptor desc);

    /**
     * Narrow a value with reference type to another (typically more specific) type.
     *
     * @param value the value to narrow
     * @param toType the type to narrow to
     * @return the narrowed type
     */
    Value narrow(Value value, ValueType toType);

    Value narrow(Value value, TypeDescriptor desc);

    // memory handles

    ValueHandle memberOf(ValueHandle structHandle, CompoundType.Member member);

    ValueHandle elementOf(ValueHandle array, Value index);

    ValueHandle pointerHandle(Value pointer);

    ValueHandle referenceHandle(Value reference);

    ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field);

    ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type);

    ValueHandle staticField(FieldElement field);

    ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type);

    ValueHandle globalVariable(GlobalVariableElement variable);

    ValueHandle localVariable(LocalVariableElement variable);

    // memory

    Value addressOf(ValueHandle handle);

    Value stackAllocate(ValueType type, Value count, Value align);

    Value new_(ClassObjectType type);

    Value new_(ClassTypeDescriptor desc);

    Value newArray(ArrayObjectType arrayType, Value size);

    Value newArray(ArrayTypeDescriptor desc, Value size);

    Value multiNewArray(ArrayObjectType arrayType, List<Value> dimensions);

    Value multiNewArray(ArrayTypeDescriptor desc, List<Value> dimensions);

    Value clone(Value object);

    Value load(ValueHandle handle, MemoryAtomicityMode mode);

    Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode);

    Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode);

    Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode);

    Node fence(MemoryAtomicityMode fenceType);

    Node monitorEnter(Value obj);

    Node monitorExit(Value obj);

    // method invocation

    Node invokeStatic(MethodElement target, List<Value> arguments);

    Node invokeStatic(TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments);

    Node invokeInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments);

    Node invokeInstance(DispatchInvocation.Kind kind, Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments);

    Node invokeDynamic(MethodElement bootstrapMethod, List<Value> staticArguments, List<Value> arguments);

    Value invokeValueStatic(MethodElement target, List<Value> arguments);

    Value invokeValueStatic(TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments);

    Value invokeValueInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments);

    Value invokeValueInstance(DispatchInvocation.Kind kind, Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments);

    Value invokeValueDynamic(MethodElement bootstrapMethod, List<Value> staticArguments, ValueType type, List<Value> arguments);

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

    Value invokeConstructor(Value instance, TypeDescriptor owner, MethodDescriptor descriptor, List<Value> arguments);

    // misc

    Value callFunction(Value callTarget, List<Value> arguments); // todo: flags etc.

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

    BasicBlock unreachable();

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

    BasicBlock try_(Triable operation, BlockLabel resumeLabel, BlockLabel exceptionHandler);

    /**
     * Terminate the block with a class cast exception.
     *
     * @param fromType the type ID of the value being cast
     * @param toType the target type ID
     * @return the terminated block
     */
    BasicBlock classCastException(Value fromType, Value toType);

    BasicBlock noSuchMethodError(ObjectType owner, MethodDescriptor desc, String name);

    BasicBlock classNotFoundError(String name);

    /**
     * Get the current block's entry node.
     *
     * @return the current block's entry node
     */
    BlockEntry getBlockEntry();

    /**
     * The policy which is used to acquire the exception handler for the current instruction.
     */
    interface ExceptionHandlerPolicy {
        /**
         * Get the currently active exception handler, if any.
         *
         * @param delegate the next-lower-priority exception handler to delegate to when the returned handler does not
         *                 handle the exception
         * @return the exception handler to use
         */
        ExceptionHandler computeCurrentExceptionHandler(ExceptionHandler delegate);
    }

    /**
     * An exception handler definition.
     */
    interface ExceptionHandler {
        /**
         * Get the block label of this handler.
         *
         * @return the block label (must not be {@code null})
         */
        BlockLabel getHandler();

        /**
         * Enter the handler from the given source block, which may be a {@code try} operation or may be a regular
         * control flow operation like {@code goto} or {@code if}. This method is always called from outside of a
         * block, thus the implementation of this method is allowed to generate instructions but any generated
         * block must be terminated before the method returns.
         *
         * @param from the source block (must not be {@code null})
         * @param exceptionValue the exception value to register (must not be {@code null})
         */
        void enterHandler(BasicBlock from, Value exceptionValue);
    }

    static BasicBlockBuilder simpleBuilder(final TypeSystem typeSystem, final ExecutableElement element) {
        return new SimpleBasicBlockBuilder(element, typeSystem);
    }
}
