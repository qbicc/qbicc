package org.qbicc.plugin.reachability;

import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * A set of hooks that enable the ReachabilityBlockBuilder to inform the underlying
 * reachability analysis engine of relevant program constructs it has encountered.
 */
interface ReachabilityAnalysis {
     void processArrayElementType(ObjectType elemType);

    void processBuildtimeInstantiatedObjectType(LoadedTypeDefinition ltd, ExecutableElement originalElement);

    void processReachableObjectLiteral(ObjectLiteral objectLiteral, ExecutableElement originalElement);

    void processReachableRuntimeInitializer(final InitializerElement target, ExecutableElement originalElement);

    void processReachableExactInvocation(final InvokableElement target, ExecutableElement originalElement);

    void processReachableDispatchedInvocation(final MethodElement target, ExecutableElement originalElement);

    void processStaticElementInitialization(final LoadedTypeDefinition ltd, BasicElement cause, ExecutableElement originalElement);

    void processClassInitialization(final LoadedTypeDefinition ltd);

    void processInstantiatedClass(final LoadedTypeDefinition type, boolean directlyInstantiated, boolean onHeapType, ExecutableElement originalElement);

    void clear();

    void reportStats();
}
