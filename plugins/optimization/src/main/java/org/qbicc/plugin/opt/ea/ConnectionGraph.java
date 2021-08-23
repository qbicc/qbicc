package org.qbicc.plugin.opt.ea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.qbicc.context.ClassContext;
import org.qbicc.graph.Call;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.Load;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.ParameterValue;
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

final class ConnectionGraph {
    private final Map<Node, Value> pointsToEdges = new HashMap<>(); // solid (P) edges
    private final Map<Node, ValueHandle> deferredEdges = new HashMap<>(); // dashed (D) edges
    private final Map<Value, Collection<InstanceFieldOf>> fieldEdges = new HashMap<>(); // solid (F) edges

    private final Map<Node, EscapeValue> escapeValues = new HashMap<>();
    private final Map<ValueHandle, New> localNewNodes = new HashMap<>();
    private final List<ParameterValue> parameters = new ArrayList<>();
    private final String name;

    ConnectionGraph(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ConnectionGraph{" +
            "name='" + name + '\'' +
            '}';
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
        addFieldEdgeIfAbsent(new_, instanceField);
        addPointsToEdgeIfAbsent(newHandle, new_);
    }

    void fixEdgesNew(ValueHandle newHandle, New new_) {
        addPointsToEdgeIfAbsent(newHandle, new_);
    }

    void fixEdgesParameterValue(ParameterValue from, InstanceFieldOf to) {
        addDeferredEdgeIfAbsent(from, to);
    }

    boolean isNoEscape(New new_) {
        return EscapeValue.isNoEscape(escapeValues.get(new_));
    }

    void propagateArgEscape() {
        final List<Node> argEscapeOnly = escapeValues.entrySet().stream()
            .filter(e -> e.getValue().isArgEscape())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Separate computing from filtering since it modifies the collection itself
        argEscapeOnly.forEach(this::computeArgEscapeOnly);
    }

    void update(Call callee, ConnectionGraph calleeCG) {
        final List<Value> arguments = callee.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            final Value outsideArg = arguments.get(i);
            final ParameterValue insideArg = calleeCG.parameters.get(i);

            final Collection<Node> mapsToNode = new ArrayList<>();
            mapsToNode.add(outsideArg);

            updateNodes(insideArg, mapsToNode, calleeCG);
        }
    }

    private void updateNodes(Node calleeNode, Collection<Node> mapsToNode, ConnectionGraph calleeCG) {
        for (Value calleePointed : calleeCG.getPointsTo(calleeNode)) {
            for (Node callerNode : mapsToNode) {
                final Collection<Value> allCallerPointed = getPointsTo(callerNode);
                if (allCallerPointed.isEmpty()) {
                    // TODO do I need to insert a new node as the target for callerNode?
                }

                for (Value callerPointed : allCallerPointed) {
                    if (mapsToNode.add(calleePointed)) {
                        // The escape state of caller nodes is marked GlobalEscape,
                        // if the escape state of the callee node is GlobalEscape.
                        mergeEscapeValue(callerPointed, calleeCG, calleePointed);

                        for (InstanceFieldOf calleeField : calleeCG.fieldEdges.get(calleePointed)) {
                            final String calleeFieldName = calleeField.getVariableElement().getName();
                            final Collection<Node> callerField = fieldEdges.get(callerPointed).stream()
                                .filter(field -> Objects.equals(field.getVariableElement().getName(), calleeFieldName))
                                .collect(Collectors.toList());

                            updateNodes(calleeField, callerField, calleeCG);
                        }
                    }
                }
            }
        }
    }

    /**
     * PointsTo(p) returns the set of of nodes that are immediately pointed by p.
     */
    private Collection<Value> getPointsTo(Node node) {
        // TODO do deferred need to be taken into account? Shouldn't ByPass have removed them?

        final ValueHandle deferredEdge = deferredEdges.get(node);
        if (deferredEdge != null) {
            final Value pointedByDeferred = pointsToEdges.get(deferredEdge);
            if (pointedByDeferred != null)
                return List.of(pointedByDeferred);
        }

        final Value pointedDirectly = pointsToEdges.get(node);
        if (pointedDirectly != null) {
            return List.of(pointedDirectly);
        }

        return Collections.emptyList();
    }

    private boolean mergeEscapeValue(Node node, ConnectionGraph otherCG, Value otherNode) {
        final EscapeValue escapeValue = escapeValues.get(node);
        final EscapeValue mergedEscapeValue = escapeValue.merge(otherCG.escapeValues.get(otherNode));
        return escapeValues.replace(node, escapeValue, mergedEscapeValue);
    }

    private void computeArgEscapeOnly(Node from) {
        // TODO filter that not global reachable

        final Node to = deferredEdges.get(from) != null
            ? deferredEdges.get(from)
            : pointsToEdges.get(from);

        if (to != null) {
            setEscapeValue(to, EscapeValue.ARG_ESCAPE);
            computeArgEscapeOnly(to);
        }
    }

    private boolean addFieldEdgeIfAbsent(New from, InstanceFieldOf to) {
        return fieldEdges
            .computeIfAbsent(from, obj -> new ArrayList<>())
            .add(to);
    }

    private boolean addPointsToEdgeIfAbsent(ValueHandle from, New to) {
        return pointsToEdges.putIfAbsent(from, to) == null;
    }

    private boolean addDeferredEdgeIfAbsent(Node from, ValueHandle to) {
        return deferredEdges.putIfAbsent(from, to) == null;
    }

    private void setEscapeValue(Node node, EscapeValue escapeValue) {
        escapeValues.put(node, escapeValue);
    }

    private EscapeValue defaultEscapeValue(ClassObjectType type, ExecutableElement element) {
        if (isSubtypeOfClass("java/lang/Thread", type, element) || isSubtypeOfClass("java/lang/ThreadGroup", type, element)) {
            return EscapeValue.GLOBAL_ESCAPE;
        }

        // TODO mark anything that is Runnable as GLOBAL_ESCAPE

        return EscapeValue.NO_ESCAPE;
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
