package org.qbicc.graph;

import java.util.List;

import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * A program graph builder, which builds each basic block in succession and wires them together.
 */
public interface BasicBlockBuilder extends Locatable {
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
     * Get the root element currently being built.
     *
     * @return the root element currently being built
     */
    ExecutableElement getRootElement();

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
     * Signal method entry with the given arguments.
     */
    void startMethod(List<ParameterValue> arguments);

    /**
     * Indicate that all construction is complete.
     */
    void finish();

    /**
     * Get the first (entry) block of the subprogram.  If the first block has not yet been terminated, an exception
     * is thrown.
     *
     * @return the first (entry) block (not {@code null})
     * @throws IllegalStateException if the first block has not yet been terminated
     */
    BasicBlock getFirstBlock() throws IllegalStateException;

    // values

    ParameterValue parameter(ValueType type, String label, int index);

    Value currentThread();

    Value offsetOfField(FieldElement fieldElement);

    // sub-value extraction

    Value extractElement(Value array, Value index);

    Value extractMember(Value compound, CompoundType.Member member);

    Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type);

    Value extractInstanceField(Value valueObj, FieldElement field);

    Value insertElement(Value array, Value index, Value value);

    Value insertMember(Value compound, CompoundType.Member member, Value value);

    // debug

    Node declareDebugAddress(LocalVariableElement variable, Value address);

    // phi

    PhiValue phi(ValueType type, BlockLabel owner, PhiValue.Flag... flags);

    // ternary

    Value select(Value condition, Value trueValue, Value falseValue);

    // binary

    Value add(Value v1, Value v2);

    Value multiply(Value v1, Value v2);

    Value and(Value v1, Value v2);

    Value or(Value v1, Value v2);

    Value xor(Value v1, Value v2); // also used for ones-complement

    Value isEq(Value v1, Value v2);

    Value isNe(Value v1, Value v2);

    Value shr(Value v1, Value v2);

    Value shl(Value v1, Value v2);

    Value sub(Value v1, Value v2); // also used for twos-complement

    Value divide(Value v1, Value v2);

    Value remainder(Value v1, Value v2);

    Value min(Value v1, Value v2);

    Value max(Value v1, Value v2);

    Value isLt(Value v1, Value v2);

    Value isGt(Value v1, Value v2);

    Value isLe(Value v1, Value v2);

    Value isGe(Value v1, Value v2);

    Value rol(Value v1, Value v2);

    Value ror(Value v1, Value v2);

    Value cmp(Value v1, Value v2);

    Value cmpG(Value v1, Value v2);

    Value cmpL(Value v1, Value v2);

    // unary

    Value notNull(Value v);

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
     * @param dims the dimensions if type ID is for reference array, 0 otherwise
     * @return a non-nullable narrowed reference to the class object for the given type ID value
     */
    Value classOf(Value typeId, Value dims);

    Value truncate(Value value, WordType toType);

    Value extend(Value value, WordType toType);

    Value bitCast(Value value, WordType toType);

    Value valueConvert(Value value, WordType toType);

    Value instanceOf(Value input, ObjectType expectedType, int expectedDimensions);

    Value instanceOf(Value input, TypeDescriptor desc);

    Value checkcast(Value value, Value toType, Value toDimensions, CheckCast.CastType kind, ObjectType expectedType);

    Value checkcast(Value value, TypeDescriptor desc);

    // memory handles

    ValueHandle memberOf(ValueHandle structHandle, CompoundType.Member member);

    ValueHandle elementOf(ValueHandle array, Value index);

    ValueHandle unsafeHandle(ValueHandle base, Value offset, ValueType outputType);

    ValueHandle pointerHandle(Value pointer);

    ValueHandle referenceHandle(Value reference);

    ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field);

    ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type);

    ValueHandle staticField(FieldElement field);

    ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type);

    ValueHandle globalVariable(GlobalVariableElement variable);

    ValueHandle localVariable(LocalVariableElement variable);

    ValueHandle exactMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType);

    ValueHandle exactMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor);

    ValueHandle virtualMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType);

    ValueHandle virtualMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor);

    ValueHandle interfaceMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType);

    ValueHandle interfaceMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor);

    ValueHandle staticMethod(MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType);

    ValueHandle staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor);

    ValueHandle constructorOf(Value instance, ConstructorElement constructor, MethodDescriptor callSiteDescriptor, FunctionType callSiteType);

    ValueHandle constructorOf(Value instance, TypeDescriptor owner, MethodDescriptor descriptor);

    ValueHandle functionOf(FunctionElement function);

    ValueHandle functionOf(Function function);

    ValueHandle functionOf(FunctionDeclaration function);

    // memory

    Value addressOf(ValueHandle handle);

    /**
     * Get a value that is a reference to the given value handle. If the handle's type is not an allocated
     * object, an exception is thrown.
     *
     * @param handle the value handle (must not be {@code null})
     * @return the reference value (not {@code null})
     * @throws IllegalArgumentException if the value handle does not refer to something that can be referenced
     */
    Value referenceTo(ValueHandle handle) throws IllegalArgumentException;

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

    Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode, CmpAndSwap.Strength strength);

    Value deref(Value value);

    Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode);

    Node classInitCheck(ObjectType objectType);

    Node fence(MemoryAtomicityMode fenceType);

    Node monitorEnter(Value obj);

    Node monitorExit(Value obj);

    // method invocation

    /**
     * Call an invocation target with normal program-order dependency behavior.  The target either does not throw an exception or
     * the current block does not catch exceptions.
     *
     * @param target the invocation target handle (must not be {@code null})
     * @param arguments the invocation arguments (must not be {@code null})
     * @return the invocation result (not {@code null})
     * @see Call
     */
    Value call(ValueHandle target, List<Value> arguments);

    /**
     * Call an invocation target that does not have side-effects (and does not have any program-order dependency relationships).
     * The target either does not throw an exception or the current block does not catch exceptions.
     *
     * @param target the invocation target handle (must not be {@code null})
     * @param arguments the invocation arguments (must not be {@code null})
     * @return the invocation result (not {@code null})
     * @see CallNoSideEffects
     */
    Value callNoSideEffects(ValueHandle target, List<Value> arguments);

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
     * Call an invocation target that does not return, thus terminating the block.
     *
     * @param target the invocation target handle (must not be {@code null})
     * @param arguments the invocation arguments (must not be {@code null})
     * @return the terminated block (not {@code null}
     * @see CallNoReturn
     */
    BasicBlock callNoReturn(ValueHandle target, List<Value> arguments);

    /**
     * Call an invocation target that does not return - thus terminating the block - and catch the thrown exception.
     *
     * @param target the invocation target handle (must not be {@code null})
     * @param arguments the invocation arguments (must not be {@code null})
     * @param catchLabel the exception handler label (must not be {@code null})
     * @return the terminated block (not {@code null}
     * @see InvokeNoReturn
     */
    BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel);

    /**
     * Tail-call an invocation target that returns the same type as this method, thus terminating the block.  The
     * backend can optimize such calls into tail calls if the calling element is {@linkplain ClassFile#I_ACC_HIDDEN hidden}.
     *
     * @param target the invocation target handle (must not be {@code null})
     * @param arguments the invocation arguments (must not be {@code null})
     * @return the terminated block (not {@code null}
     * @see TailCall
     */
    BasicBlock tailCall(ValueHandle target, List<Value> arguments);

    /**
     * Tail-call an invocation target that returns the same type as this method - thus terminating the block - and catch
     * the thrown exception.  The backend can optimize such calls into tail calls if the calling element is
     * {@linkplain ClassFile#I_ACC_HIDDEN hidden}.
     *
     * @param target the invocation target handle (must not be {@code null})
     * @param arguments the invocation arguments (must not be {@code null})
     * @param catchLabel the exception handler label (must not be {@code null})
     * @return the terminated block (not {@code null}
     * @see TailInvoke
     */
    BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel);

    /**
     * Call an invocation target and catch the thrown exception, terminating the block.
     * <b>Note</b>: the terminated block is not returned.
     * The return value of this method is the return value of the invocation,
     * which will always be pinned to the {@code resumeLabel} block.
     *
     * @param target the invocation target handle (must not be {@code null})
     * @param arguments the invocation arguments (must not be {@code null})
     * @param catchLabel the exception handler label (must not be {@code null})
     * @param resumeLabel the handle of the resume target (must not be {@code null})
     * @return the invocation result (not {@code null})
     * @see Invoke
     */
    Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel);

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

    /**
     * Get the current block's entry node.
     *
     * @return the current block's entry node
     */
    BlockEntry getBlockEntry();

    /**
     * Get the most-recently-terminated block.
     *
     * @return the most recently terminated block (not {@code null})
     * @throws IllegalStateException if no block has yet been terminated
     */
    BasicBlock getTerminatedBlock();

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
         * @param from the source block of the {@code throw} or {@code invoke} (must not be {@code null})
         * @param landingPad the landing pad block, or {@code null} if the value was thrown directly
         * @param exceptionValue the exception value to register (must not be {@code null})
         */
        void enterHandler(BasicBlock from, BasicBlock landingPad, Value exceptionValue);
    }

    static BasicBlockBuilder simpleBuilder(final TypeSystem typeSystem, final ExecutableElement element) {
        return new SimpleBasicBlockBuilder(element, typeSystem);
    }
}
