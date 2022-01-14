package org.qbicc.plugin.coreclasses;

import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.element.FieldElement;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

/**
 * Initialize manually-created interpreter objects in the same way as {@link BasicHeaderInitializer}.
 */
public class BasicHeaderManualInitializer implements Consumer<VmObject> {
    private final CompilationContext ctxt;

    public BasicHeaderManualInitializer(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    @Override
    public void accept(VmObject vmObject) {
        // start with the basic stuff
        ClassObjectType objectTypeId = vmObject.getObjectTypeId();
        Memory memory = vmObject.getMemory();
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        FieldElement typeIdField = coreClasses.getObjectTypeIdField();
        memory.storeType(vmObject.indexOf(typeIdField), objectTypeId, SinglePlain);
        if (vmObject instanceof VmArray) {
            FieldElement lengthField = coreClasses.getArrayLengthField();
            memory.store32(vmObject.indexOf(lengthField), ((VmArray) vmObject).getLength(), SinglePlain);
            if (vmObject instanceof VmReferenceArray) {
                ReferenceArrayObjectType arrayType = ((VmReferenceArray) vmObject).getObjectType();
                FieldElement elemTypeIdField = coreClasses.getRefArrayElementTypeIdField();
                memory.storeType(vmObject.indexOf(elemTypeIdField), arrayType.getLeafElementType(), SinglePlain);
                FieldElement dimensionsField = coreClasses.getRefArrayDimensionsField();
                memory.store8(vmObject.indexOf(dimensionsField), arrayType.getDimensionCount(), SinglePlain);
            }
        }
    }
}
