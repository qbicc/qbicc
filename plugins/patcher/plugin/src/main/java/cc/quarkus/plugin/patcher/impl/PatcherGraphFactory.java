package cc.quarkus.plugin.patcher.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.graph.ArrayType;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.DelegatingGraphFactory;
import cc.quarkus.qcc.graph.GraphFactory;
import cc.quarkus.qcc.graph.InstanceInvocation;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.graph.Value;
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
    private final Map<ClassType, Map<String, MappedField>> patchFields = new ConcurrentHashMap<>();
    /**
     * A mapping of patched fields to accessors.
     */
    private final Map<ClassType, Map<String, ClassType>> accessors = new ConcurrentHashMap<>();
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

    public Value instanceOf(final Context ctxt, final Value v, final ClassType type) {
        return super.instanceOf(ctxt, v, remapType(type));
    }

    public Value new_(final Context ctxt, final ClassType type) {
        return super.new_(ctxt, remapType(type));
    }

    public Value newArray(final Context ctxt, final ArrayType type, final Value size) {
        return super.newArray(ctxt, remapType(type), size);
    }

    public Value readInstanceField(final Context ctxt, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
        ClassType accessor = accessors.getOrDefault(owner, Map.of()).get(name);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            final MappedField mapped = patchFields.getOrDefault(owner, Map.of()).get(name);
            if (mapped != null) {
                return readInstanceField(ctxt, instance, mapped.classType, mapped.name, mode);
            } else {
                return super.readInstanceField(ctxt, instance, remapType(owner), name, mode);
            }
        }
    }

    public Value readStaticField(final Context ctxt, final ClassType owner, final String name, final JavaAccessMode mode) {
        ClassType accessor = accessors.getOrDefault(owner, Map.of()).get(name);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            final MappedField mapped = patchFields.getOrDefault(owner, Map.of()).get(name);
            if (mapped != null) {
                return readStaticField(ctxt, mapped.classType, mapped.name, mode);
            } else {
                return super.readStaticField(ctxt, remapType(owner), name, mode);
            }
        }
    }

    public Node writeInstanceField(final Context ctxt, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        ClassType accessor = accessors.getOrDefault(owner, Map.of()).get(name);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            final MappedField mapped = patchFields.getOrDefault(owner, Map.of()).get(name);
            if (mapped != null) {
                return writeInstanceField(ctxt, instance, mapped.classType, mapped.name, value, mode);
            } else {
                return super.writeInstanceField(ctxt, instance, remapType(owner), name, value, mode);
            }
        }
    }

    public Node writeStaticField(final Context ctxt, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        ClassType accessor = accessors.getOrDefault(owner, Map.of()).get(name);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            final MappedField mapped = patchFields.getOrDefault(owner, Map.of()).get(name);
            if (mapped != null) {
                return writeStaticField(ctxt, mapped.classType, mapped.name, value, mode);
            } else {
                return super.writeStaticField(ctxt, remapType(owner), name, value, mode);
            }
        }
    }

    public Node invokeMethod(final Context ctxt, final ParameterizedExecutableElement target, final List<Value> arguments) {
        return super.invokeMethod(ctxt, remapMethod(target), arguments);
    }

    public Node invokeInstanceMethod(final Context ctxt, final Value instance, final InstanceInvocation.Kind kind, final ParameterizedExecutableElement target, final List<Value> arguments) {
        return super.invokeInstanceMethod(ctxt, instance, kind, remapMethod(target), arguments);
    }

    public Value invokeValueMethod(final Context ctxt, final MethodElement target, final List<Value> arguments) {
        return super.invokeValueMethod(ctxt, (MethodElement) remapMethod(target), arguments);
    }

    public Value invokeInstanceValueMethod(final Context ctxt, final Value instance, final InstanceInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
        return super.invokeInstanceValueMethod(ctxt, instance, kind, (MethodElement) remapMethod(target), arguments);
    }

    static final class MappedField {
        final ClassType classType;
        final String name;

        MappedField(final ClassType classType, final String name) {
            this.classType = classType;
            this.name = name;
        }
    }
}
