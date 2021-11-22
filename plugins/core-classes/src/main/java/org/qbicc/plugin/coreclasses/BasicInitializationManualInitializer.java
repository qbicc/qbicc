package org.qbicc.plugin.coreclasses;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmReferenceArrayClass;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.element.FieldElement;

/**
 * Initialize manually-created interpreter objects in the same way as {@link BasicInitializationBasicBlockBuilder}.
 */
public class BasicInitializationManualInitializer implements Consumer<VmObject> {
    private final CompilationContext ctxt;

    public BasicInitializationManualInitializer(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void accept(VmObject vmObject) {
        // start with the basic stuff
        ClassObjectType objectTypeId = vmObject.getObjectTypeId();
        Memory memory = vmObject.getMemory();
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        FieldElement typeIdField = coreClasses.getObjectTypeIdField();
        memory.storeType(vmObject.indexOf(typeIdField), objectTypeId, MemoryAtomicityMode.UNORDERED);
        FieldElement nativeMonitorField = coreClasses.getObjectNativeObjectMonitorField();
        memory.storeRef(vmObject.indexOf(nativeMonitorField), null, MemoryAtomicityMode.UNORDERED);
        if (vmObject instanceof VmArray) {
            FieldElement lengthField = coreClasses.getArrayLengthField();
            memory.store32(vmObject.indexOf(lengthField), ((VmArray) vmObject).getLength(), MemoryAtomicityMode.UNORDERED);
            if (vmObject instanceof VmReferenceArray) {
                ReferenceArrayObjectType arrayType = ((VmReferenceArray) vmObject).getObjectType();
                FieldElement elemTypeIdField = coreClasses.getRefArrayElementTypeIdField();
                memory.storeType(vmObject.indexOf(elemTypeIdField), arrayType.getLeafElementType(), MemoryAtomicityMode.UNORDERED);
                FieldElement dimensionsField = coreClasses.getRefArrayDimensionsField();
                memory.store8(vmObject.indexOf(dimensionsField), arrayType.getDimensionCount(), MemoryAtomicityMode.UNORDERED);
            }
        } else if (vmObject instanceof VmClass) {
            if (! (vmObject instanceof VmPrimitiveClass)) {
                FieldElement instanceTypeIdField = coreClasses.getClassTypeIdField();
                if (vmObject instanceof VmReferenceArrayClass) {
                    VmReferenceArrayClass vmArray = (VmReferenceArrayClass) vmObject;
                    ReferenceArrayObjectType arrayType = vmArray.getInstanceObjectType();
                    FieldElement dimsField = coreClasses.getClassDimensionField();
                    memory.store8(vmObject.indexOf(dimsField), arrayType.getDimensionCount(), MemoryAtomicityMode.UNORDERED);
                    memory.storeType(vmObject.indexOf(instanceTypeIdField), vmArray.getLeafTypeId(), MemoryAtomicityMode.UNORDERED);
                } else {
                    memory.storeType(vmObject.indexOf(instanceTypeIdField), ((VmClass) vmObject).getInstanceObjectTypeId(), MemoryAtomicityMode.UNORDERED);
                }
            }
        }
    }
}
