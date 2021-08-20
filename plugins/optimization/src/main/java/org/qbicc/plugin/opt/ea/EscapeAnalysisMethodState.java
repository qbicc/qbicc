package org.qbicc.plugin.opt.ea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.qbicc.context.ClassContext;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.Load;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

// TODO try to make method parameter types as concrete as possible to avoid future problems
final class EscapeAnalysisMethodState {
    private final ConnectionGraph connectionGraph = new ConnectionGraph();
    private final Map<Node, EscapeValue> escapeValues = new HashMap<>();
    private final Map<ValueHandle, New> localNewNodes = new HashMap<>();
    private final List<ParameterValue> parameters = new ArrayList<>();
    private final String name;

    EscapeAnalysisMethodState(String name) {
        this.name = name;
    }

    void trackLocalNew(LocalVariable localHandle, New new_) {
        localNewNodes.put(localHandle, new_);
    }

    void trackNew(New new_, ClassObjectType type, ExecutableElement element) {
        setEscapeValue(new_, defaultEscapeValue(type, element));
    }

    void trackParameters(List<ParameterValue> args) {
        parameters.addAll(args);
        parameters.forEach(arg -> setEscapeValue(arg, EscapeValue.ARG_ESCAPE));
    }

    void trackReturn(Value value) {
        if (value instanceof Load) {
            final Value localNew = localNewNodes.get(value.getValueHandle());
            if (localNew != null) {
                setEscapeValue(localNew, EscapeValue.ARG_ESCAPE);
                return;
            }
        }

        setEscapeValue(value, EscapeValue.ARG_ESCAPE);
    }

    void trackStoreStaticField(Value value) {
        setEscapeValue(value, EscapeValue.GLOBAL_ESCAPE);
    }

    void fixEdgesField(New new_, ValueHandle newHandle, InstanceFieldOf instanceField) {
        connectionGraph.addFieldEdgeIfAbsent(new_, instanceField);
        connectionGraph.addPointsToEdgeIfAbsent(newHandle, new_);
    }

    void fixEdgesNew(ValueHandle newHandle, New new_) {
        connectionGraph.addPointsToEdgeIfAbsent(newHandle, new_);
    }

    void fixEdgesParameterValue(ParameterValue from, InstanceFieldOf to) {
        connectionGraph.addDeferredEdgeIfAbsent(from, to);
    }

    void propagateArgEscape() {
        final List<Node> argEscapeOnly = reachableFromArgEscapeOnly();
        // Separate computing from filtering since it modifies the collection itself
        argEscapeOnly.forEach(node -> setEscapeValue(node, EscapeValue.ARG_ESCAPE));
    }

    ParameterValue getParameter(int index) {
        return parameters.get(index);
    }

    Collection<Value> getPointsTo(Node node) {
        return connectionGraph.getPointsTo(node);
    }

    Collection<InstanceFieldOf> getFieldEdges(Value value) {
        return connectionGraph.getFieldEdges(value);
    }

    boolean isNoEscape(New new_) {
        return EscapeValue.isNoEscape(escapeValues.get(new_));
    }

    boolean mergeEscapeValue(Node node, EscapeAnalysisMethodState otherState, Value otherNode) {
        final EscapeValue escapeValue = escapeValues.get(node);
        final EscapeValue mergedEscapeValue = escapeValue.merge(otherState.escapeValues.get(otherNode));
        return escapeValues.replace(node, escapeValue, mergedEscapeValue);
    }

    @Override
    public String toString() {
        return "EscapeAnalysis{" +
            "name='" + name + '\'' +
            '}';
    }

    private List<Node> reachableFromArgEscapeOnly() {
        return escapeValues.entrySet().stream()
            .filter(e -> e.getValue().isArgEscape()) // TODO check that not reachable from a GlobalEscape node
            .map(Map.Entry::getKey)
            .flatMap(node -> connectionGraph.getReachableFrom(node).stream())
            .collect(Collectors.toList());
    }

    private EscapeValue defaultEscapeValue(ClassObjectType type, ExecutableElement element) {
        if (isSubtypeOfClass("java/lang/Thread", type, element) || isSubtypeOfClass("java/lang/ThreadGroup", type, element)) {
            return EscapeValue.GLOBAL_ESCAPE;
        }

        return EscapeValue.NO_ESCAPE;
    }

    private void setEscapeValue(Node node, EscapeValue escapeValue) {
        escapeValues.put(node, escapeValue);
    }

    private boolean isSubtypeOfClass(String name, ClassObjectType type, ExecutableElement element) {
        // TODO would using ctxt.getBootstrapClassContext() work?
        final ClassContext classContext = element.getEnclosingType().getContext();

        // TODO can you use resolveTypeFromClassName?
        final ClassTypeDescriptor targetDesc = ClassTypeDescriptor.synthesize(classContext, name);
        final ValueType targetType = classContext.resolveTypeFromDescriptor(
            targetDesc, TypeParameterContext.of(element), TypeSignature.synthesize(classContext, targetDesc), TypeAnnotationList.empty(), TypeAnnotationList.empty());

        if (targetType instanceof ReferenceType) {
            ObjectType upperBound = ((ReferenceType) targetType).getUpperBound();
            return type.isSubtypeOf(upperBound);
        }

        return false;
    }
}
