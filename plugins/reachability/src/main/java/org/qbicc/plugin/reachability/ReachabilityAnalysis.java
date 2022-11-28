package org.qbicc.plugin.reachability;

import org.qbicc.interpreter.VmObject;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;

/**
 * A set of hooks that enable the ReachabilityBlockBuilder to inform the underlying
 * reachability analysis engine of relevant program constructs it has encountered.
 */
interface ReachabilityAnalysis {
     void processArrayElementType(ObjectType elemType);

    void processBuildtimeInstantiatedObjectType(LoadedTypeDefinition ltd, ExecutableElement currentElement);

    void processReachableObject(VmObject objectLiteral, ExecutableElement currentElement);

    void processReachableRuntimeInitializer(final InitializerElement target, ExecutableElement currentElement);

    void processReachableExactInvocation(final InvokableElement target, ExecutableElement currentElement);

    void processReachableDispatchedInvocation(final MethodElement target, ExecutableElement currentElement);

    void processReachableStaticFieldAccess(final StaticFieldElement field, ExecutableElement currentElement);

    void processReachableType(final LoadedTypeDefinition ltd, ExecutableElement currentElement);

    void processInstantiatedClass(final LoadedTypeDefinition type, boolean onHeapType, ExecutableElement currentElement);

    void clear();

    void reportStats();
}
