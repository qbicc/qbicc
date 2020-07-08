package cc.quarkus.vm.implementation;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BinaryValue;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.CommutativeBinaryValue;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.IfValue;
import cc.quarkus.qcc.graph.InstanceFieldWrite;
import cc.quarkus.qcc.graph.InstanceOfValue;
import cc.quarkus.qcc.graph.Invocation;
import cc.quarkus.qcc.graph.InvocationValue;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.MemoryState;
import cc.quarkus.qcc.graph.NewValue;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NonCommutativeBinaryValue;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Throw;
import cc.quarkus.qcc.graph.Try;
import cc.quarkus.qcc.graph.UnaryValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.type.definition.DefinedMethodDefinition;
import cc.quarkus.qcc.type.definition.PreparedTypeDefinition;
import cc.quarkus.qcc.type.definition.ResolvedFieldDefinition;
import cc.quarkus.qcc.type.definition.ResolvedMethodBody;
import cc.quarkus.qcc.type.definition.ResolvedMethodDefinition;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import cc.quarkus.qcc.type.universe.Universe;
import cc.quarkus.vm.api.JavaClass;
import cc.quarkus.vm.api.JavaConstructor;
import cc.quarkus.vm.api.JavaMethod;
import cc.quarkus.vm.api.JavaObject;
import cc.quarkus.vm.api.JavaThread;
import cc.quarkus.vm.api.JavaVM;

final class JavaThreadImpl implements JavaThread {
    final JavaVMImpl vm;
    State state = State.RUNNING;
    Thread attachedThread;
    Lock threadLock = new ReentrantLock();

    JavaThreadImpl(final String threadName, final JavaObject threadGroup, final boolean daemon, final JavaVMImpl vm) {
        this.vm = vm;
    }

    public void doAttached(final Runnable r) {
        threadLock.lock();
        try {
            if (state != State.RUNNING) {
                throw new IllegalStateException("Thread is not running");
            }
            if (attachedThread != null) {
                throw new IllegalStateException("Thread is already attached");
            }
            attachedThread = Thread.currentThread();
        } finally {
            threadLock.unlock();
        }
        try {
            r.run();
        } finally {
            threadLock.lock();
            assert attachedThread == Thread.currentThread();
            attachedThread = null;
            threadLock.unlock();
        }
    }

    void checkThread() {
        threadLock.lock();
        try {
            if (attachedThread != Thread.currentThread()) {
                throw new IllegalStateException("Thread is not attached");
            }
        } finally {
            threadLock.unlock();
        }
    }

    public void initClass(final JavaClass clazz) {
        checkThread();
        // todo: we have no duplicate init protection whatsoever
        PreparedTypeDefinition prepared = clazz.getTypeDefinition().resolve().prepare();
        // find the initializer
        int cnt = prepared.getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            ResolvedMethodDefinition methodDefinition = prepared.getMethodDefinition(i);
            if (methodDefinition.getName().equals("<clinit>")) {
                // the one and only
                executeExact(methodDefinition);
                return;
            }
        }
        // class has no initializer (OK)
    }

    public JavaClass defineClass(final String name, final JavaObject classLoader, final ByteBuffer bytes) {
        Universe dictionary = vm.getDictionaryFor(classLoader);
        VerifiedTypeDefinition def = dictionary.defineClass(name, bytes).verify();
        JavaClassImpl javaClass = new JavaClassImpl(vm, def);
        vm.registerJavaClassOf(def.getClassType(), javaClass);
        return javaClass;
    }

    private static final AtomicLong anonCounter = new AtomicLong();

    public JavaClass defineAnonymousClass(final JavaClass hostClass, final ByteBuffer bytes) {
        String newName = hostClass.getTypeDefinition().getName() + "/" + anonCounter.getAndIncrement();
        return defineClass(newName, vm.getClassLoaderFor(hostClass.getTypeDefinition().getDefiningClassLoader()), bytes);
    }

    public JavaObject allocateObject(final JavaClass type) {
        return new JavaObjectImpl((JavaClassImpl) type);
    }

    public void initObject(final JavaObject newObj, final JavaConstructor ctor, final Object... args) {
        executeExact(ctor, newObj, args);
    }

    public JavaConstructor lookupConstructor(final JavaClass type, final String name, final String signature) {
        throw new UnsupportedOperationException();
    }

    public JavaMethod lookupStaticMethod(final JavaClass type, final String name, final String signature) {
        throw new UnsupportedOperationException();
    }

    public JavaMethod lookupInstanceMethod(final JavaClass type, final String name, final String signature) {
        throw new UnsupportedOperationException();
    }

    public void callStaticVoid(final JavaMethod method, final Object... args) {
        throw new UnsupportedOperationException();
    }

    private JavaObject executeExact(JavaMethod method, JavaObject receiver, Object... args) {
        checkThread();
        DefinedMethodDefinition definition = method.getDefinition();
        return executeExact(definition, args);
    }

    private JavaObject executeExact(final DefinedMethodDefinition definition, final Object... args) {
        ResolvedMethodBody resolved = definition.resolve().getMethodBody().verify().resolve();
        BasicBlock entryBlock = resolved.getEntryBlock();
        // todo: cache
        Schedule schedule = Schedule.forMethod(entryBlock);
        List<ParameterValue> parameters = resolved.getParameters();
        if (args.length != parameters.size()) {
            throw new IllegalArgumentException("Mismatched parameter lengths");
        }
        Map<Value, Object> values = new HashMap<>();
        int i = 0;
        for (ParameterValue value : parameters) {
            values.put(value, args[i++]);
        }
        return execute(null, schedule, entryBlock, values);
    }

    private JavaObject execute(final BasicBlock predecessor, final Schedule schedule, final BasicBlock block, final Map<Value, Object> values) {
        Set<Node> executed = new HashSet<>();
        Terminator terminator = block.getTerminator();
        return execute(predecessor, schedule, block, terminator, values, executed);
    }

    private JavaObject execute(final BasicBlock predecessor, final Schedule schedule, final BasicBlock block, final Node instruction, final Map<Value, Object> values, final Set<Node> executed) {
        // recompute all nodes in the current block
        if (schedule.getBlockForNode(instruction) != block || ! executed.add(instruction)) {
            return null;
        }
        // first execute all instruction dependencies
        int cnt = instruction.getValueDependencyCount();
        for (int i = 0; i < cnt; i ++) {
            execute(predecessor, schedule, block, instruction.getValueDependency(i), values, executed);
        }
        if (instruction instanceof MemoryState) {
            MemoryState dependency = ((MemoryState) instruction).getMemoryDependency();
            if (dependency != null) {
                execute(predecessor, schedule, block, dependency, values, executed);
            }
        }
        // now execute this instruction
        if (instruction instanceof Terminator) {
            // control transfer of some kind; most of these are not values (only invocation is)
            Terminator terminator = (Terminator) instruction;
            if (terminator instanceof Try) {
                Try try_ = (Try) terminator;
                if (try_ instanceof Throw) {
                    // could throw and catch, but why?
                    values.put(try_.getCatchValue(), ((Throw) try_).getThrownValue());
                    return execute(block, schedule, try_.getCatchHandler(), values);
                }
                try {
                    return executeInvocation(predecessor, schedule, block, (Invocation) instruction, values);
                } catch (Thrown thrown) {
                    JavaObject throwable = thrown.getThrowable();
                    values.put(try_.getCatchValue(), throwable);
                    return execute(block, schedule, try_.getCatchHandler(), values);
                }
            }
            if (terminator instanceof Throw) {
                throw new Thrown((JavaObject) values.get(((Throw) terminator).getThrownValue()));
            }
            if (terminator instanceof Goto) {
                // continue to the next block
                return execute(block, schedule, ((Goto) terminator).getNextBlock(), values);
            }
            if (terminator instanceof If) {
                If if_ = (If) terminator;
                // calculate condition
                Object cond = values.get(if_.getCondition());
                if (cond.equals(Boolean.TRUE)) {
                    return execute(block, schedule, if_.getTrueBranch(), values);
                } else {
                    return execute(block, schedule, if_.getFalseBranch(), values);
                }
            }
        } else if (instruction instanceof Value) {
            // something that yields a value; some of these might also be MemoryStates
            Value value = (Value) instruction;
            Object result;
            if (value instanceof PhiValue) {
                result = values.get(((PhiValue) value).getValueForBlock(predecessor));
            } else if (value instanceof BinaryValue) {
                BinaryValue binaryValue = (BinaryValue) value;
                Object left = values.get(binaryValue.getLeftInput());
                Object right = values.get(binaryValue.getRightInput());
                if (binaryValue instanceof CommutativeBinaryValue) {
                    CommutativeBinaryValue.Kind kind = ((CommutativeBinaryValue) binaryValue).getKind();
                    switch (kind) {
                        case ADD: {
                            result = executeAdd(left, right);
                            break;
                        }
                        case MULTIPLY: {
                            result = executeMultiply(left, right);
                            break;
                        }
                        case AND:
                        case OR:
                        case XOR:
                        case CMP_EQ:
                        case CMP_NE:
                        default: {
                            throw new IllegalStateException();
                        }
                    }
                } else if (binaryValue instanceof NonCommutativeBinaryValue) {
                    NonCommutativeBinaryValue.Kind kind = ((NonCommutativeBinaryValue) binaryValue).getKind();
                    switch (kind) {
                        case UNSIGNED_SHR:
                        case SHR:
                        case SHL:
                        case SUB:
                        case CMP_GE:
                        case CMP_LE:
                        case CMP_GT:
                        case CMP_LT:
                        case MOD:
                        case DIV:
                        default: {
                            throw new IllegalStateException();
                        }
                    }
                } else {
                    throw new IllegalStateException();
                }
            } else if (value instanceof UnaryValue) {
                UnaryValue unaryValue = (UnaryValue) value;
                UnaryValue.Kind kind = unaryValue.getKind();
                Object input = values.get(unaryValue.getInput());
                switch (kind) {
                    case NEGATE: {
                        result = executeNegate(input);
                        break;
                    }
                    default: {
                        throw new IllegalStateException();
                    }
                }
            } else if (value instanceof InvocationValue) {
                result = executeInvocation(predecessor, schedule, block, (InvocationValue) value, values);
            } else if (value instanceof IfValue) {
                IfValue ifValue = (IfValue) value;
                Object cond = values.get(ifValue.getCondition());
                if (cond.equals(Boolean.TRUE)) {
                    result = values.get(ifValue.getTrueValue());
                } else {
                    result = values.get(ifValue.getFalseValue());
                }
            } else if (value instanceof InstanceOfValue) {
                InstanceOfValue instanceOfValue = (InstanceOfValue) value;
                ClassType instanceType = instanceOfValue.getInstanceType();
                JavaObject instance = (JavaObject) values.get(instanceOfValue.getInstance());
                ClassType javaClass = instance.getJavaClass().getTypeDefinition().getClassType();
                result = Boolean.valueOf(instanceType.isSuperTypeOf(javaClass));
            } else if (value instanceof NewValue) {
                NewValue newValue = (NewValue) value;
                ClassType type = newValue.getType();
                final JavaClassImpl classObj = vm.getJavaClassOf(type);
                result = new JavaObjectImpl(classObj);
            } else {
                throw new IllegalStateException();
            }
            values.put(value, result);
        } else if (instruction instanceof MemoryState) {
            if (instruction instanceof InstanceFieldWrite) {
                InstanceFieldWrite instanceFieldWrite = (InstanceFieldWrite) instruction;
                JavaObjectImpl instance = (JavaObjectImpl) values.get(instanceFieldWrite.getInstance());
                Object writeValue = values.get(instanceFieldWrite.getWriteValue());
                if (writeValue instanceof JavaObject) {
                    JavaAccessMode mode = instanceFieldWrite.getMode();
                    String fieldName = instanceFieldWrite.getFieldName();
                    if (mode == JavaAccessMode.DETECT) {
                        ResolvedFieldDefinition fieldDefinition = instanceFieldWrite.getFieldOwner().getDefinition().resolve().findField(fieldName);
                        mode = fieldDefinition.isVolatile() ? JavaAccessMode.VOLATILE : JavaAccessMode.PLAIN;
                    }
                    // obviously terrible
                    switch (mode) {
                        case PLAIN: {
                            instance.fields.setFieldPlain(fieldName, (JavaObject) writeValue);
                            break;
                        }
                        case ORDERED: {
                            // todo: probably wrong, figure it out
                            instance.fields.setFieldRelease(fieldName, (JavaObject) writeValue);
                            break;
                        }
                        case VOLATILE: {
                            instance.fields.setFieldVolatile(fieldName, (JavaObject) writeValue);
                            break;
                        }
                        default: {
                            throw new IllegalStateException();
                        }
                    }
                } else {
                    throw new IllegalStateException();
                }
            }
        } else {
            throw new IllegalStateException();
        }
        return null;
    }

    private Object executeAdd(final Object left, final Object right) {
        // this is obviously a terrible approach
        if (left instanceof Integer) {
            return Integer.valueOf(((Integer) left).intValue() + ((Integer) right).intValue());
        } else {
            throw new IllegalStateException();
        }
    }

    private Object executeMultiply(final Object left, final Object right) {
        if (left instanceof Integer) {
            return Integer.valueOf(((Integer) left).intValue() * ((Integer) right).intValue());
        } else {
            throw new IllegalStateException();
        }
    }

    private Object executeNegate(final Object input) {
        if (input instanceof Integer) {
            return Integer.valueOf(-((Integer) input).intValue());
        } else {
            throw new IllegalStateException();
        }
    }

    private JavaObject executeInvocation(final BasicBlock predecessor, final Schedule schedule, final BasicBlock block, final Invocation instruction, final Map<Value, Object> values) {
        return null;
    }

    public JavaVM getVM() {
        return vm;
    }

    public String getStringRegion(final JavaObject string, final int offs, final int len) {
        return null;
    }

    public boolean isRunning() {
        threadLock.lock();
        try {
            return state == State.RUNNING;
        } finally {
            threadLock.unlock();
        }
    }

    public boolean isFinished() {
        threadLock.lock();
        try {
            return state == State.FINISHED;
        } finally {
            threadLock.unlock();
        }
    }

    public void close() {
        threadLock.lock();
        try {
            throw new UnsupportedOperationException();
        } finally {
            threadLock.unlock();
        }
    }

    enum State {
        RUNNING,
        FINISHED,
        ;
    }
}
