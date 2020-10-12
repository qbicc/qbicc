package cc.quarkus.plugin.patcher.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.graph.ArrayType;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.DelegatingGraphFactory;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.GraphFactory;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;

final class PatcherGraphFactory extends DelegatingGraphFactory implements GraphFactory {
    /**
     * A mapping of patch-to-patched class types.
     */
    private final Map<ClassType, ClassType> patchTypes = new ConcurrentHashMap<>();
    /**
     * A mapping of patch-to-patched fields.
     */
    private final Map<FieldElement, FieldElement> patchFields = new ConcurrentHashMap<>();
    /**
     * A mapping of patched fields to accessors.
     */
    private final Map<FieldElement, ClassType> accessors = new ConcurrentHashMap<>();
    /**
     * A mapping of methods to their replacements.
     */
    private final Map<ParameterizedExecutableElement, ParameterizedExecutableElement> patchMethods = new ConcurrentHashMap<>();

    PatcherGraphFactory(final GraphFactory delegate) {
        super(delegate);
    }

    @SuppressWarnings("unchecked")
    private <T extends Type> T remapType(T original) {
        if (original instanceof ClassType) {
            final ClassType classType = (ClassType) original;
            return (T) patchTypes.getOrDefault(classType, classType);
        } else {
            return original;
        }
    }

    private ParameterizedExecutableElement remapMethod(ParameterizedExecutableElement original) {
        return patchMethods.getOrDefault(original, original);
    }

    public Value instanceOf(final Context ctxt, final Value value, final ClassType type) {
        return super.instanceOf(ctxt, value, remapType(type));
    }

    public Value new_(final Context ctxt, final ClassType type) {
        return super.new_(ctxt, remapType(type));
    }

    public Value newArray(final Context ctxt, final ArrayType type, final Value size) {
        return super.newArray(ctxt, remapType(type), size);
    }

    public Value readInstanceField(final Context ctxt, final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
        ClassType accessor = accessors.get(fieldElement);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            FieldElement mapped = patchFields.get(fieldElement);
            if (mapped != null) {
                return readInstanceField(ctxt, instance, mapped, mode);
            } else {
                return super.readInstanceField(ctxt, instance, fieldElement, mode);
            }
        }
    }

    public Value readStaticField(final Context ctxt, final FieldElement fieldElement, final JavaAccessMode mode) {
        ClassType accessor = accessors.get(fieldElement);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            FieldElement mapped = patchFields.get(fieldElement);
            if (mapped != null) {
                return readStaticField(ctxt, mapped, mode);
            } else {
                return super.readStaticField(ctxt, fieldElement, mode);
            }
        }
    }

    public Node writeInstanceField(final Context ctxt, final Value instance, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        ClassType accessor = accessors.get(fieldElement);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            FieldElement mapped = patchFields.get(fieldElement);
            if (mapped != null) {
                return writeInstanceField(ctxt, instance, mapped, value, mode);
            } else {
                return super.writeInstanceField(ctxt, instance, fieldElement, value, mode);
            }
        }
    }

    public Node writeStaticField(final Context ctxt, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        ClassType accessor = accessors.get(fieldElement);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            FieldElement mapped = patchFields.get(fieldElement);
            if (mapped != null) {
                return writeStaticField(ctxt, fieldElement, value, mode);
            } else {
                return super.writeStaticField(ctxt, fieldElement, value, mode);
            }
        }
    }

    public Node invokeStatic(final Context ctxt, final MethodElement target, final List<Value> arguments) {
        return super.invokeStatic(ctxt, (MethodElement) remapMethod(target), arguments);
    }

    public Node invokeInstance(final Context ctxt, final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return super.invokeInstance(ctxt, kind, instance, (MethodElement) remapMethod(target), arguments);
    }

    public Value invokeValueStatic(final Context ctxt, final MethodElement target, final List<Value> arguments) {
        return super.invokeValueStatic(ctxt, (MethodElement) remapMethod(target), arguments);
    }

    public Value invokeInstanceValueMethod(final Context ctxt, final Value instance, final DispatchInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
        return super.invokeInstanceValueMethod(ctxt, instance, kind, (MethodElement) remapMethod(target), arguments);
    }

    public Value invokeConstructor(final Context ctxt, final Value instance, final ConstructorElement target, final List<Value> arguments) {
        return super.invokeConstructor(ctxt, instance, (ConstructorElement) remapMethod(target), arguments);
    }
}
