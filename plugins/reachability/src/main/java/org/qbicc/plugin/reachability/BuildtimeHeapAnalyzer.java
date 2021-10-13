package org.qbicc.plugin.reachability;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This class supports reachability analysis by providing the capability of
 * tracing the build-time instantiated heap starting from the static fields
 * of a reachable LoadedTypeDefinition to identify reachable instantiated types.
 * It internally tracks which objects have already been visited and avoids re-visiting
 * them, since revisiting an object cannot make additional types reachable.
 * It also does skips over instance fields that cannot add reachable types (primitives,
 * java.lang.Class instances, and java.lang.String instances).
 */
class BuildtimeHeapAnalyzer {
    private final Map<VmObject, Boolean> visited = Collections.synchronizedMap(new IdentityHashMap<>());

    void clear() {
        visited.clear();
    }

    /**
     * Trace the build-time heap starting from the statics of the given type
     * to identify instantiated types.
     * @param ltd Type whose static fields are the roots for this trace
     */
    void traceHeap(CompilationContext ctxt, RTAInfo rtaInfo, LoadedTypeDefinition ltd) {
        ArrayDeque<VmObject> worklist = new ArrayDeque<>();

        int fieldCount = ltd.getFieldCount();
        for (int i=0; i<fieldCount; i++) {
            FieldElement f = ltd.getField(i);
            if (f.isStatic() && f.getType() instanceof ReferenceType) {
                Value v = ltd.getInitialValue(f);
                if (v instanceof ObjectLiteral) {
                    VmObject vo = ((ObjectLiteral) v).getValue();
                    if (!visited.containsKey(vo)) {
                        worklist.add(vo);
                        visited.put(vo, Boolean.TRUE);
                    }
                }
            }
        }

        Layout interpreterLayout = Layout.getForInterpreter(ctxt);
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        while (!worklist.isEmpty()) {
            VmObject cur = worklist.pop();

            PhysicalObjectType ot = cur.getObjectType();
            if (ot instanceof ClassObjectType && !(cur instanceof VmClass || cur instanceof VmString)) {
                LoadedTypeDefinition concreteType = cur.getObjectType().getDefinition().load();
                rtaInfo.processBuildtimeInstantiatedObjectType(concreteType, ltd);

                LayoutInfo memLayout = interpreterLayout.getInstanceLayoutInfo(concreteType);
                for (CompoundType.Member im : memLayout.getCompoundType().getMembers()) {
                    if (im.getType() instanceof ReferenceType) {
                        VmObject child = cur.getMemory().loadRef(im.getOffset(), MemoryAtomicityMode.UNORDERED);
                        if (child != null && !visited.containsKey(child)) {
                            worklist.add(child);
                            visited.put(child, Boolean.TRUE);
                        }
                    }
                }
            } else if (ot instanceof ReferenceArrayObjectType) {
                rtaInfo.processArrayElementType(((ReferenceArrayObjectType) ot).getLeafElementType());

                FieldElement contentsField = coreClasses.getRefArrayContentField();
                LayoutInfo info = interpreterLayout.getInstanceLayoutInfo(contentsField.getEnclosingType());
                Memory memory = cur.getMemory();
                int length = memory.load32(info.getMember(coreClasses.getArrayLengthField()).getOffset(), MemoryAtomicityMode.UNORDERED);
                for (int i=0; i<length; i++) {
                    VmObject e = memory.loadRef(((VmArray) cur).getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED);
                    if (e != null && !visited.containsKey(e)) {
                        worklist.add(e);
                        visited.put(e, Boolean.TRUE);
                    }
                }
            }
        }
    }
}
