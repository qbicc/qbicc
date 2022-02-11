package org.qbicc.plugin.opt.ea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.qbicc.graph.Call;
import org.qbicc.graph.CastValue;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.Load;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.New;
import org.qbicc.graph.Node;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;

final class ConnectionGraph {
    // TODO Handle situations where a node has multiple points-to.
    //      Even if a reference is potentially assigned multiple New nodes (e.g. branches), the refs are currently different.

    /**
     * Points-to are edges from references to objects referenced.
     * The references are {@link ValueHandle} instances, e.g. {@link ReferenceHandle}, {@link StaticField}...etc.
     * Referenced objects are normally {@link New} instances, but they can also be {@link ParameterValue} or {@link Call}.
     * <p>
     * Each method's connection graph tracks this during intra method analysis:
     * <p><ul>
     * <li>The points-to edges are used to transfer information from callee methods to caller methods.
     *    For example, imagine that a method {@code A} creates a new {@code Boo} and assigns it to variable {@code x}.
     *    Then, method {@code A} sends {@code x} as argument to method {@code B}.
     *    If method {@code B} assigns the parameter to a static variable, the parameter reference is marked as global escape.
     *    However, method {@code B} in itself doesn't know about the new {@code Boo} instance.
     * </li>
     * <li>Then, when a method analysis completes in inter-method analysis,
     *    for any references that are global escape, objects referenced also become global escape.
     *    Following the example in the previous point, the parameter reference has been marked as global escape.
     *    This gets mapped to the reference that points to the New instance, which in turn becomes global escape.
     *    This happens during inter method analysis because caller method escape value can switch to global escape,
     *    e.g. if a callee method assigns an argument to a static field.
     * </li>
     * </ul><p>
     */
    private final Map<Node, Node> pointsToEdges = new HashMap<>(); // solid (P) edges

    /**
     * A deferred edge from a node {@code p} to a node {@code q} means that {@code p} points to whatever {@code q} points to.
     * They model assignments that copy references from one to another during intra method analysis.
     * They're mostly used to link {@link ParameterValue} instances with their uses.
     * During inter method analysis these are bypassed recursively to establish points to edges.
     */
    private final Map<Node, ValueHandle> deferredEdges = new HashMap<>(); // dashed (D) edges
    private final Map<Node, Collection<InstanceFieldOf>> fieldEdges = new HashMap<>(); // solid (F) edges

    /**
     * Tracks escape value of graph nodes.
     * It includes not only {@link Value} nodes but also {@link ValueHandle} nodes.
     * The escape value of handles gets propagated eventually, using points-to edges, to {@code Value} instances.
     */
    private final Map<Node, EscapeValue> escapeValues = new HashMap<>();

    /**
     * This helps overcome the lack of direct link between {@link LocalVariable} and {@link New} nodes in the graph.
     */
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

    void trackNew(New new_, EscapeValue escapeValue) {
        setEscapeValue(new_, escapeValue);
    }

    void trackParameters(List<ParameterValue> args) {
        parameters.addAll(args);
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

    void trackStoreStaticField(ValueHandle handle, Value value) {
        addPointsToEdgeIfAbsent(handle, value);
        setEscapeValue(handle, EscapeValue.GLOBAL_ESCAPE);
    }

    void trackStoreThisField(Value value) {
        setEscapeValue(value, EscapeValue.ARG_ESCAPE);
    }

    void trackThrowNew(New value) {
        // New allocations thrown assumed to escape as arguments
        // TODO Could it be possible to only mark as argument escaping those that escape the method?
        setEscapeValue(value, EscapeValue.ARG_ESCAPE);
    }
    
    void trackCast(CastValue cast) {
        addPointsToEdgeIfAbsent(cast, cast.getInput());
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

    /**
     * Returns the escape value associated with the given node.
     * If the node is not found, its escape value is unknown.
     */
    EscapeValue getEscapeValue(Node node) {
        return EscapeValue.of(escapeValues.get(node));
    }

    /**
     * Returns the field nodes associated with the give node.
     * If the node is not found, an empty collection is returned.
     */
    Collection<InstanceFieldOf> getFields(Node node) {
         final Collection<InstanceFieldOf> fields = fieldEdges.get(node);
         return Objects.isNull(fields) ? Collections.emptyList() : fields;
    }

    ValueHandle getDeferred(Node node) {
        return deferredEdges.get(node);
    }

    void updateAtMethodEntry() {
        // Set all parameters as arg escape
        parameters.forEach(arg -> setEscapeValue(arg, EscapeValue.ARG_ESCAPE));
    }

    void updateAfterInvokingMethod(Call callee, ConnectionGraph calleeCG) {
        // TODO this should really be removed, no method called that is not reachable should make it here
        if (callee.getArguments().size() > calleeCG.parameters.size())
            return;

        final List<Value> arguments = callee.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
            final Value outsideArg = arguments.get(i);
            final ParameterValue insideArg = calleeCG.parameters.get(i);
            updateCallerNodes(insideArg, List.of(outsideArg), calleeCG, new ArrayList<>());
        }
    }

    private void updateCallerNodes(Node calleeNode, Collection<Node> mapsToField, ConnectionGraph calleeCG, Collection<Node> mapsToObj) {
        // TODO includeSelf only needed for ParameterValue nodes, otherwise it's wasteful
        //      consider alternative based on phantom nodes or self references
        for (Node calleePointed : calleeCG.getPointsTo(calleeNode, true)) {
            for (Node callerNode : mapsToField) {
                for (Node callerPointed : getPointsTo(callerNode, true)) {
                    if (mapsToObj.add(calleePointed)) {
                        // The escape state of caller nodes is marked GlobalEscape,
                        // if the escape state of the callee node is GlobalEscape.
                        if (calleeCG.getEscapeValue(calleePointed).isGlobalEscape()) {
                            setEscapeValue(callerPointed, EscapeValue.GLOBAL_ESCAPE);
                        }

                        for (InstanceFieldOf calleeField : calleeCG.getFields(calleePointed)) {
                            final String calleeFieldName = calleeField.getVariableElement().getName();
                            final Collection<Node> callerFields = getFields(callerPointed).stream()
                                .filter(field -> Objects.equals(field.getVariableElement().getName(), calleeFieldName))
                                .collect(Collectors.toList());

                            updateCallerNodes(calleeField, callerFields, calleeCG, mapsToObj);
                        }
                    }
                }
            }
        }
    }

    void updateAtMethodExit() {
        // Use by pass function to eliminate all deferred edges in the CG
        bypassAllDeferredEdges(deferredEdges);

        // Mark all nodes reachable from a global escape nodes as global escape.
        propagateGlobalEscape();

        // Mark all nodes reachable from arg escape nodes, but not global escape, as arg escape.
        propagateArgEscapeOnly();
    }

    private void bypassAllDeferredEdges(Map<Node, ValueHandle> oldDeferredEdges) {
        if (oldDeferredEdges.isEmpty()) {
            deferredEdges.clear();
            return;
        }

        Map<Node, ValueHandle> newDeferredEdges = new HashMap<>();
        for (ValueHandle node : oldDeferredEdges.values()) {
            final ValueHandle defersTo = oldDeferredEdges.get(node);
            final Node pointsTo = pointsToEdges.get(node);
            if (defersTo != null || pointsTo != null) {
                for (Map.Entry<Node, ValueHandle> incoming : oldDeferredEdges.entrySet()) {
                    if (incoming.getValue().equals(node)) {
                        if (defersTo != null) {
                            newDeferredEdges.put(incoming.getKey(), defersTo);
                        }
                        if (pointsTo != null) {
                            addPointsToEdgeIfAbsent(incoming.getKey(), pointsTo);
                        }
                    }
                }
            }
        }

        bypassAllDeferredEdges(newDeferredEdges);
    }

    private void propagateGlobalEscape() {
        final List<Node> argEscapeOnly = escapeValues.entrySet().stream()
            .filter(e -> e.getValue().isGlobalEscape())
            .map(Map.Entry::getKey)
            .toList();

        // Separate computing from filtering since it modifies the collection itself
        argEscapeOnly.forEach(this::computeGlobalEscape);
    }

    private void computeGlobalEscape(Node from) {
        final Node to = pointsToEdges.get(from);

        if (to != null) {
            setEscapeValue(to, EscapeValue.GLOBAL_ESCAPE);
            computeGlobalEscape(to);
        }
    }

    void propagateArgEscapeOnly() {
        final List<Node> argEscapeOnly = escapeValues.entrySet().stream()
            .filter(e -> e.getValue().isArgEscape())
            .map(Map.Entry::getKey)
            .toList();

        // Separate computing from filtering since it modifies the collection itself
        argEscapeOnly.forEach(this::computeArgEscapeOnly);
    }

    private void computeArgEscapeOnly(Node from) {
        final Node to = pointsToEdges.get(from);

        if (to != null && getEscapeValue(to).notGlobalEscape()) {
            setEscapeValue(to, EscapeValue.ARG_ESCAPE);
            computeArgEscapeOnly(to);
        }
    }

    /**
     * PointsTo(p) returns the set of nodes that are immediately pointed by p.
     * If includeSelf is true, the set also includes p, otherwise it won't be present.
     */
    Collection<Node> getPointsTo(Node node, boolean includeSef) {
        final Node pointsTo = pointsToEdges.get(node);
        return pointsTo != null
            ? includeSef ? List.of(node, pointsTo) : List.of(pointsTo)
            : includeSef ? List.of(node) : List.of();
    }

    ConnectionGraph union(ConnectionGraph other) {
        if (Objects.nonNull(other)) {
            this.pointsToEdges.putAll(other.pointsToEdges);
            this.deferredEdges.putAll(other.deferredEdges);
            this.fieldEdges.putAll(other.fieldEdges);

            final Map<Node, EscapeValue> mergedEscapeValues = mergeEscapeValues(other);
            this.escapeValues.clear();
            this.escapeValues.putAll(mergedEscapeValues);

            this.localNewNodes.putAll(other.localNewNodes);
            this.parameters.addAll(other.parameters);
        }

        return this;
    }

    void resolveReturnedPhiValues() {
        final List<Value> possibleNewValues = this.escapeValues.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof PhiValue && entry.getValue().isArgEscape())
            .flatMap(entry -> ((PhiValue) entry.getKey()).getPossibleValues().stream())
            .filter(value -> value instanceof New && getEscapeValue(value).isNoEscape())
            .toList();

        // Separate computing from filtering since it modifies the collection itself
        possibleNewValues.forEach(value -> setEscapeValue(value, EscapeValue.ARG_ESCAPE));
    }

    /**
     * Validate the escape state value of New nodes in the connection graph.
     * If New nodes exist that are not amongst the supported ones,
     * their escape state value must be pessimistically set to global escape.
     *
     * When data flow graphs are not fully handled,
     * any New nodes found along the way must be pessimistically set their escape state value to global.
     * This is done to avoid relying on escape state values that haven't been calculated precisely enough.
     * When precision cannot be guaranteed, the escape estate value cannot be relied upon.
     *
     * This method assumes that only no escape, or argument escape, verified New nodes are passed in.
     */
    void validateNewNodes(List<New> supported) {
        final List<Node> unsupportedNewNodes = this.escapeValues.entrySet().stream()
            // Find all non-global escape nodes in the connection graph
            .filter(e -> e.getKey() instanceof New && e.getValue().notGlobalEscape())
            // Find those that are not verified
            .filter(e -> !supported.contains(e.getKey()))
            .map(Map.Entry::getKey)
            .toList();

        // Unverified nodes get their escape state pessimistically set to global escape
        unsupportedNewNodes.forEach(node -> setEscapeValue(node, EscapeValue.GLOBAL_ESCAPE));
    }

    private Map<Node, EscapeValue> mergeEscapeValues(ConnectionGraph other) {
        final Map<Node, EscapeValue> result = new HashMap<>(this.escapeValues);
        other.escapeValues.forEach((key, value) -> result.merge(key, value, EscapeValue::merge));
        return result;
    }

    private boolean addFieldEdgeIfAbsent(New from, InstanceFieldOf to) {
        return fieldEdges
            .computeIfAbsent(from, obj -> new ArrayList<>())
            .add(to);
    }

    private boolean addPointsToEdgeIfAbsent(Node from, Node to) {
        return pointsToEdges.putIfAbsent(from, to) == null;
    }

    private boolean addDeferredEdgeIfAbsent(Node from, ValueHandle to) {
        return deferredEdges.putIfAbsent(from, to) == null;
    }

    private void setEscapeValue(Node node, EscapeValue escapeValue) {
        escapeValues.put(node, escapeValue);
    }
}
