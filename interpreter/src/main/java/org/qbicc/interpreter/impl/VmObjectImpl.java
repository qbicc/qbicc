package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.memory.MemoryFactory;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.StructType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
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
    final Memory memory;
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
     * A general object attachment used by VM-side implementations.
     */
    volatile Object attachment;

    /**
     * Construct a new instance.
     *
     * @param clazz the class of this object (must not be {@code null})
     */
    VmObjectImpl(final VmClassImpl clazz) {
        this.clazz = clazz;
        memory = clazz.getVmClass().getVm().allocate(clazz.getLayoutInfo().getStructType(), 1);
    }

    /**
     * Construct a new array instance.
     * @param clazz the array class (must not be {@code null})
     * @param arrayMemory the array memory (must not be {@code null})
     */
    VmObjectImpl(final VmArrayClassImpl clazz, final Memory arrayMemory) {
        this.clazz = clazz;
        memory = MemoryFactory.compose(clazz.getVm().allocate(clazz.getLayoutInfo().getStructType(), 1), arrayMemory);
    }

    /**
     * Special ctor for Class.class, whose clazz instance is itself.
     */
    VmObjectImpl(VmImpl vm, @SuppressWarnings("unused") Class<?> unused, LayoutInfo instanceLayoutInfo) {
        this.clazz = (VmClassImpl) this;
        memory = vm.allocate(instanceLayoutInfo.getStructType(), 1);
    }

    /**
     * Cloning constructor.
     *
     * @param original the original
     */
    VmObjectImpl(VmObjectImpl original) {
        clazz = original.clazz;
        memory = original.memory.clone();
    }

    @Override
    public Memory getMemory() {
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
        LayoutInfo layoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(loaded);
        StructType.Member member = layoutInfo.getMember(field);
        if (member == null) {
            throw new IllegalArgumentException("Field " + field + " is not present on " + this);
        }
        return member.getOffset();
    }

    public void setRefField(LoadedTypeDefinition owner, String name, VmObject value) {
        getMemory().storeRef(indexOf(owner.findField(name)), value, SinglePlain);
    }

    public void setPointerField(LoadedTypeDefinition owner, String name, Pointer value) {
        getMemory().storePointer(indexOf(owner.findField(name)), value, SinglePlain);
    }

    public void setPointerField(LoadedTypeDefinition owner, String name, long value) {
        FieldElement field = owner.findField(name);
        getMemory().storePointer(indexOf(field), new IntegerAsPointer((PointerType) field.getType(), value), SinglePlain);
    }

    public void setTypeField(LoadedTypeDefinition owner, String name, ValueType value) {
        getMemory().storeType(indexOf(owner.findField(name)), value, SinglePlain);
    }

    public void setBooleanField(LoadedTypeDefinition owner, String name, boolean value) {
        // todo: switch on type.getMinBits()
        setByteField(owner, name, value ? 1 : 0);
    }

    public void setByteField(LoadedTypeDefinition owner, String name, int value) {
        getMemory().store8(indexOf(owner.findField(name)), value, SinglePlain);
    }

    public void setShortField(LoadedTypeDefinition owner, String name, int value) {
        getMemory().store16(indexOf(owner.findField(name)), value, SinglePlain);
    }

    public void setIntField(LoadedTypeDefinition owner, String name, int value) {
        getMemory().store32(indexOf(owner.findField(name)), value, SinglePlain);
    }

    public void setFloatField(LoadedTypeDefinition owner, String name, float value) {
        setIntField(owner, name, Float.floatToRawIntBits(value));
    }

    public void setLongField(LoadedTypeDefinition owner, String name, long value) {
        FieldElement field = owner.findField(name);
        if (field.getType() instanceof PointerType pt) {
            setPointerField(owner, name, new IntegerAsPointer(pt, value));
        } else {
            getMemory().store64(indexOf(field), value, SinglePlain);
        }
    }

    public void setDoubleField(LoadedTypeDefinition owner, String name, double value) {
        setLongField(owner, name, Double.doubleToRawLongBits(value));
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

    public <T> T getOrAddAttachment(Class<T> clazz, Supplier<T> supplier) {
        Object attachment = this.attachment;
        if (attachment == null) {
            synchronized (this) {
                attachment = this.attachment;
                if (attachment == null) {
                    this.attachment = attachment = supplier.get();
                }
            }
        }
        return clazz.cast(attachment);
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

    protected VmObjectImpl clone() {
        return new VmObjectImpl(this);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    StringBuilder toString(StringBuilder target) {
        VmThread vmThread = Vm.currentThread();
        if (vmThread != null) {
            try {
                VmImpl vm = (VmImpl) vmThread.getVM();
                VmString str = (VmString) vm.invokeVirtual(vm.toStringMethod, this, List.of());
                target.append(str.getContent());
            } catch (Exception e) {
                target.append(clazz.getName()).append(' ');
                target.append("(toString failed: ").append(e).append(')');
            }
        }
        return target;
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
