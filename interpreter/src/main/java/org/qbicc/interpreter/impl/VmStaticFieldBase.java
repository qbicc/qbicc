package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmStaticFieldBaseObject;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
final class VmStaticFieldBase implements VmStaticFieldBaseObject, Referenceable {
    private final VmClassImpl vmClass;
    private final LayoutInfo staticLayout;
    private final MemoryImpl memory;

    /**
     * Construct a new instance.
     *
     * @param vmClass the class for which this object exists
     * @param staticLayout the static layout
     * @param memory the static memory for the class
     */
    VmStaticFieldBase(VmClassImpl vmClass, LayoutInfo staticLayout, MemoryImpl memory) {
        this.vmClass = vmClass;
        this.staticLayout = staticLayout;
        this.memory = memory;
    }

    @Override
    public VmClass getEnclosedVmClass() {
        return vmClass;
    }

    @Override
    public VmClass getVmClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PhysicalObjectType getObjectType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassObjectType getObjectTypeId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MemoryImpl getMemory() {
        return memory;
    }

    @Override
    public void monitorEnter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void monitorExit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void vmWait() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void vmWait(long millis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void vmWait(long millis, int nanos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void vmNotify() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void vmNotifyAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(FieldElement field) throws IllegalArgumentException {
        if (field.isStatic()) {
            CompoundType.Member member = staticLayout.getMember(field);
            if (member != null) {
                return member.getOffset();
            }
        }
        throw new IllegalArgumentException();
    }
}
