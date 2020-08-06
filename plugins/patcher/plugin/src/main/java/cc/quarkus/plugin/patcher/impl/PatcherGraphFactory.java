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
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;

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

    private MethodIdentifier remapMethod(MethodIdentifier original) {
        Type orig = original.getReturnType();
        Type remapped = remapType(orig);
        if (orig != remapped) {
            return MethodIdentifier.of(original.getName(), remapDescriptor(original));
        }
        final int cnt = original.getParameterCount();
        for (int i = 0; i < cnt; i++) {
            orig = original.getParameterType(i);
            remapped = remapType(orig);
            if (orig != remapped) {
                return MethodIdentifier.of(original.getName(), remapDescriptor(original));
            }
        }
        return original;
    }

    private MethodTypeDescriptor remapDescriptor(final MethodTypeDescriptor original) {
        final int cnt = original.getParameterCount();
        if (cnt == 0) {
            return MethodTypeDescriptor.of(remapType(original.getReturnType()));
        }
        Type[] types = new Type[cnt];
        for (int i = 0; i < cnt; i++) {
            types[i] = remapType(original.getParameterType(i));
        }
        return MethodTypeDescriptor.of(remapType(original.getReturnType()), types);
    }

    public Value instanceOf(final Value v, final ClassType type) {
        return super.instanceOf(v, remapType(type));
    }

    public Value reinterpretCast(final Value v, final Type type) {
        return super.reinterpretCast(v, remapType(type));
    }

    public Value new_(final Node dependency, final ClassType type) {
        return super.new_(dependency, remapType(type));
    }

    public Value newArray(final Node dependency, final ArrayType type, final Value size) {
        return super.newArray(dependency, remapType(type), size);
    }

    public Value readInstanceField(final Node dependency, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
        ClassType accessor = accessors.getOrDefault(owner, Map.of()).get(name);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            final MappedField mapped = patchFields.getOrDefault(owner, Map.of()).get(name);
            if (mapped != null) {
                return readInstanceField(dependency, instance, mapped.classType, mapped.name, mode);
            } else {
                return super.readInstanceField(dependency, instance, remapType(owner), name, mode);
            }
        }
    }

    public Value readStaticField(final Node dependency, final ClassType owner, final String name, final JavaAccessMode mode) {
        ClassType accessor = accessors.getOrDefault(owner, Map.of()).get(name);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            final MappedField mapped = patchFields.getOrDefault(owner, Map.of()).get(name);
            if (mapped != null) {
                return readStaticField(dependency, mapped.classType, mapped.name, mode);
            } else {
                return super.readStaticField(dependency, remapType(owner), name, mode);
            }
        }
    }

    public Node writeInstanceField(final Node dependency, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        ClassType accessor = accessors.getOrDefault(owner, Map.of()).get(name);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            final MappedField mapped = patchFields.getOrDefault(owner, Map.of()).get(name);
            if (mapped != null) {
                return writeInstanceField(dependency, instance, mapped.classType, mapped.name, value, mode);
            } else {
                return super.writeInstanceField(dependency, instance, remapType(owner), name, value, mode);
            }
        }
    }

    public Node writeStaticField(final Node dependency, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        ClassType accessor = accessors.getOrDefault(owner, Map.of()).get(name);
        if (accessor != null) {
            throw new UnsupportedOperationException("TODO: Look up or create accessor singleton with constant fold API");
        } else {
            final MappedField mapped = patchFields.getOrDefault(owner, Map.of()).get(name);
            if (mapped != null) {
                return writeStaticField(dependency, mapped.classType, mapped.name, value, mode);
            } else {
                return super.writeStaticField(dependency, remapType(owner), name, value, mode);
            }
        }
    }

    public Node invokeMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return super.invokeMethod(dependency, remapType(owner), remapMethod(method), arguments);
    }

    public Node invokeInstanceMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return super.invokeInstanceMethod(dependency, instance, kind, remapType(owner), remapMethod(method), arguments);
    }

    public Value invokeValueMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return super.invokeValueMethod(dependency, remapType(owner), remapMethod(method), arguments);
    }

    public Value invokeInstanceValueMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return super.invokeInstanceValueMethod(dependency, instance, kind, remapType(owner), remapMethod(method), arguments);
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
