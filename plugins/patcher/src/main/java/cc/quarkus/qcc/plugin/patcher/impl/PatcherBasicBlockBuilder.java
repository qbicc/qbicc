package cc.quarkus.qcc.plugin.patcher.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.ReferenceArrayObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InvokableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

final class PatcherBasicBlockBuilder extends DelegatingBasicBlockBuilder implements BasicBlockBuilder {
    /**
     * A mapping of patch-to-patched class types.
     */
    private final Map<ClassObjectType, ClassObjectType> patchTypes = new ConcurrentHashMap<>();
    /**
     * A mapping of patch-to-patched fields.
     */
    private final Map<FieldElement, FieldElement> patchFields = new ConcurrentHashMap<>();
    /**
     * A mapping of patched fields to accessors.
     */
    private final Map<FieldElement, ClassObjectType> accessors = new ConcurrentHashMap<>();
    /**
     * A mapping of methods to their replacements.
     */
    private final Map<InvokableElement, InvokableElement> patchMethods = new ConcurrentHashMap<>();
    private final CompilationContext ctxt;

    PatcherBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @SuppressWarnings("unchecked")
    private <T extends Type> T remapType(T original) {
        if (original instanceof ClassObjectType) {
            return (T) patchTypes.getOrDefault(original, (ClassObjectType) original);
        } else if (original instanceof ReferenceArrayObjectType) {
            return (T) remapType(((ReferenceArrayObjectType) original).getElementObjectType()).getReferenceArrayObject();
        } else if (original instanceof ReferenceType) {
            return (T) remapType(((ReferenceType) original).getUpperBound()).getReference();
        } else {
            return original;
        }
    }

    private InvokableElement remapMethod(InvokableElement original) {
        return patchMethods.getOrDefault(original, original);
    }

    public Value new_(final ClassObjectType type) {
        return super.new_(remapType(type));
    }

    public Value newArray(final ArrayObjectType arrayType, final Value size) {
        return super.newArray(remapType(arrayType), size);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement fieldElement) {
        ClassObjectType accessor = accessors.get(fieldElement);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            FieldElement mapped = patchFields.get(fieldElement);
            if (mapped != null) {
                return instanceFieldOf(instance, mapped);
            } else {
                return super.instanceFieldOf(instance, fieldElement);
            }
        }
    }

    @Override
    public ValueHandle staticField(FieldElement fieldElement) {
        ClassObjectType accessor = accessors.get(fieldElement);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            FieldElement mapped = patchFields.get(fieldElement);
            if (mapped != null) {
                return staticField(mapped);
            } else {
                return super.staticField(fieldElement);
            }
        }
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        return super.invokeStatic((MethodElement) remapMethod(target), arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return super.invokeInstance(kind, instance, (MethodElement) remapMethod(target), arguments);
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        return super.invokeValueStatic((MethodElement) remapMethod(target), arguments);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return super.invokeValueInstance(kind, instance, (MethodElement) remapMethod(target), arguments);
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        return super.invokeConstructor(instance, (ConstructorElement) remapMethod(target), arguments);
    }
}
