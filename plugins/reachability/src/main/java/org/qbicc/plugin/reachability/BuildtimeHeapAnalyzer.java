package org.qbicc.plugin.reachability;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmStaticFieldBaseObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.pointer.ConstructorPointer;
import org.qbicc.pointer.InstanceMethodPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.StructType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.StaticFieldElement;

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
     * Trace the build-time heap starting from a given static field
     * to identify instantiated types.
     * @param f static field which is the starting point for this trace
     */
    void traceHeap(CompilationContext ctxt, ReachabilityAnalysis analysis, StaticFieldElement f, ExecutableElement currentElement) {
        if (f.isStatic() && f.getType() instanceof ReferenceType && !f.isThreadLocal() && f.getRunTimeInitializer() == null) {
            Value v = f.getEnclosingType().load().getInitialValue(f);
            if (v instanceof ObjectLiteral ol) {
                traceHeap(ctxt, analysis, ol.getValue(), currentElement);
            }
        }
    }

    /**
     * Trace the build-time heap starting from a given VmObject
     * to identify instantiated types.
     * @param root The VmObject which is the starting point for this trace.
     */
    void traceHeap(CompilationContext ctxt, ReachabilityAnalysis analysis, VmObject root, ExecutableElement rootElement) {
        if (visited.containsKey(root)) {
            return;
        }
        visited.put(root, Boolean.TRUE);
        ArrayDeque<VmObject> worklist = new ArrayDeque<>();
        worklist.add(root);

        Layout interpreterLayout = Layout.get(ctxt);
        TypeSystem ts = ctxt.getTypeSystem();
        while (!worklist.isEmpty()) {
            VmObject cur = worklist.pop();

            if (cur instanceof VmStaticFieldBaseObject) {
                // skip
                continue;
            }

            if (cur instanceof VmThrowable t) {
                t.prepareForSerialization();
            }

            PhysicalObjectType ot = cur.getObjectType();
            if (ot instanceof ClassObjectType && !(cur instanceof VmString)) {
                LoadedTypeDefinition concreteType = cur.getObjectType().getDefinition().load();
                analysis.processBuildtimeInstantiatedObjectType(concreteType, rootElement);

                LayoutInfo memLayout = interpreterLayout.getInstanceLayoutInfo(concreteType);
                for (StructType.Member im : memLayout.getStructType().getMembers()) {
                    if (im.getType() instanceof ReferenceType) {
                        VmObject child = cur.getMemory().loadRef(im.getOffset(), SinglePlain);
                        if (child != null && !visited.containsKey(child)) {
                            worklist.add(child);
                            visited.put(child, Boolean.TRUE);
                        }
                    } else if (im.getType() instanceof PointerType || im.getType().equals(ts.getSignedInteger64Type())) {
                        Pointer pointer = cur.getMemory().loadPointer(im.getOffset(), SinglePlain);
                        if (pointer instanceof StaticMethodPointer smp) {
                            analysis.processReachableExactInvocation(smp.getExecutableElement(), rootElement);
                        } else if (pointer instanceof StaticFieldPointer sfp) {
                            analysis.processReachableStaticFieldAccess(sfp.getStaticField(), rootElement);
                        } else if (pointer instanceof InstanceMethodPointer imp) {
                            analysis.processReachableExactInvocation(imp.getExecutableElement(), rootElement);
                        } else if (pointer instanceof ConstructorPointer cp) {
                            analysis.processInstantiatedClass(cp.getExecutableElement().getEnclosingType().load(), false, rootElement);
                            analysis.processReachableExactInvocation(cp.getExecutableElement(), rootElement);
                        }
                    }
                }

                if (cur instanceof VmClass vc) {
                    // Ensure that this class gets assigned a typeID
                    analysis.processReachableType(vc.getTypeDefinition(), rootElement);
                }
            } else if (ot instanceof ReferenceArrayObjectType) {
                analysis.processArrayElementType(((ReferenceArrayObjectType) ot).getLeafElementType());

                for (VmObject e : ((VmReferenceArray) cur).getArray()) {
                    if (e != null && !visited.containsKey(e)) {
                        worklist.add(e);
                        visited.put(e, Boolean.TRUE);
                    }
                }
            }
        }
    }
}
