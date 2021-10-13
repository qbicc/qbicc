package org.qbicc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmObject;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

class VmObjectImpl implements VmObject, Referenceable {
    private static final VarHandle lockHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "lock", VarHandle.class, VmObjectImpl.class, Lock.class);
    private static final VarHandle condHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "cond", VarHandle.class, VmObjectImpl.class, Condition.class);
    private static final long MAX_MILLIS = Long.MAX_VALUE / 1_000_000L;

    /**
     * This is the class of this object (i.e. what is returned by `Object.getClass()`).
     */
    final VmClassImpl clazz;
    /**
     * This is the backing memory of this object, as defined by {@link VmClassImpl#getLayoutInfo() clazz.layoutInfo}.
     */
    final MemoryImpl memory;
    /**
     * This is the object monitor, which is lazily instantiated.
     */
    @SuppressWarnings("unused") // lockHandle
    volatile Lock lock;
    /**
     * This is the object condition, which is lazily instantiated from the monitor.
     */
    @SuppressWarnings("unused") // condHandle
    volatile Condition cond;

    /**
     * Construct a new instance.
     *
     * @param clazz the class of this object (must not be {@code null})
     */
    VmObjectImpl(final VmClassImpl clazz) {
        this.clazz = clazz;
        memory = clazz.getVmClass().getVm().allocate((int) clazz.getLayoutInfo().getCompoundType().getSize());
    }

    /**
     * Construct a new array instance.
     * @param clazz the array class (must not be {@code null})
     * @param arraySize the size of the array
     */
    VmObjectImpl(final VmArrayClassImpl clazz, final int arraySize) {
        this.clazz = clazz;
        ArrayObjectType arrayType = clazz.getInstanceObjectType();
        memory = clazz.getVm().allocate((int) (clazz.getLayoutInfo().getCompoundType().getSize() + arraySize * arrayType.getElementType().getSize()));
    }

    /**
     * Special ctor for Class.class, whose clazz instance is itself.
     */
    VmObjectImpl(VmImpl vm, @SuppressWarnings("unused") Class<?> unused, LayoutInfo instanceLayoutInfo) {
        this.clazz = (VmClassImpl) this;
        memory = vm.allocate((int) instanceLayoutInfo.getCompoundType().getSize());
    }

    @Override
    public MemoryImpl getMemory() {
        return memory;
    }

    @Override
    public void monitorEnter() {
        getLock().lock();
    }

    @Override
    public void monitorExit() {
        getLock().unlock();
    }

    @Override
    public void vmNotify() {
        getCondition().signal();
    }

    @Override
    public void vmNotifyAll() {
        getCondition().signalAll();
    }

    @Override
    public int indexOf(FieldElement field) throws IllegalArgumentException {
        LoadedTypeDefinition loaded = field.getEnclosingType().load();
        CompilationContext ctxt = loaded.getContext().getCompilationContext();
        LayoutInfo layoutInfo = Layout.getForInterpreter(ctxt).getInstanceLayoutInfo(loaded);
        CompoundType.Member member = layoutInfo.getMember(field);
        if (member == null) {
            throw new IllegalArgumentException("Field " + field + " is not present on " + this);
        }
        return member.getOffset();
    }

    @Override
    public void vmWait() throws InterruptedException {
        getCondition().await();
    }

    @Override
    public void vmWait(long millis) throws InterruptedException {
        getCondition().await(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void vmWait(long millis, int nanos) throws InterruptedException {
        Condition cond = getCondition();
        if (nanos == 0) {
            cond.await(millis, TimeUnit.MILLISECONDS);
        } else if (millis < MAX_MILLIS) {
            cond.await(millis * 1_000_000L + nanos, TimeUnit.NANOSECONDS);
        } else {
            cond.await(millis + 1, TimeUnit.MILLISECONDS);
        }
    }

    public VmClassImpl getVmClass() {
        return clazz;
    }

    public PhysicalObjectType getObjectType() {
        return clazz.getTypeDefinition().getClassType();
    }

    @Override
    public ClassObjectType getObjectTypeId() {
        return (ClassObjectType) clazz.getInstanceObjectTypeId();
    }

    Lock getLock() {
        Lock lock = (Lock) lockHandle.get(this);
        if (lock == null) {
            lock = new ReentrantLock();
            Lock appearing;
            while (! lockHandle.compareAndSet(this, null, lock)) {
                appearing = (Lock) lockHandle.get(this);
                if (appearing != null) {
                    lock = appearing;
                    break;
                }
            }
        }
        return lock;
    }

    Condition getCondition() {
        Condition cond = (Condition) condHandle.get(this);
        if (cond == null) {
            cond = getLock().newCondition();
            Condition appearing;
            while (! condHandle.compareAndSet(this, null, cond)) {
                appearing = (Condition) condHandle.get(this);
                if (appearing != null) {
                    cond = appearing;
                    break;
                }
            }
        }
        return cond;
    }
}
