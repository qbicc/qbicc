package org.qbicc.type.definition;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.Value;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.Primitive;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.definition.element.ParameterElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.MethodSignature;
import org.qbicc.type.generic.TypeSignature;

/**
 *
 */
final class LoadedTypeDefinitionImpl extends DelegatingDefinedTypeDefinition implements LoadedTypeDefinition {
    private static final VarHandle vmClassHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "vmClass", VarHandle.class, LoadedTypeDefinitionImpl.class, VmClass.class);

    private final ValueType type;
    private final DefinedTypeDefinitionImpl delegate;
    private final LoadedTypeDefinition superType;
    private final LoadedTypeDefinition[] interfaces;
    private final ArrayList<FieldElement> fields;
    private final MethodElement[] methods;
    private final MethodElement[] instanceMethods;
    private final ConstructorElement[] ctors;
    private final InitializerElement init;
    private final NestedClassElement enclosingClass;
    private final NestedClassElement[] enclosedClasses;
    private final DefinedTypeDefinition nestHost;
    private final DefinedTypeDefinition[] nestMembers;
    private int typeId = -1;
    private int maximumSubtypeId = -1;
    private final boolean hasDefaultMethods;
    private final boolean declaresDefaultMethods;
    private volatile VmClass vmClass;
    private LoadedTypeDefinition enclosingMethodClass;
    private MethodElement enclosingMethod;
    private volatile Map<MethodElement, Map<MethodDescriptor, MethodElement>> sigPolyMethods = null;

    LoadedTypeDefinitionImpl(final DefinedTypeDefinitionImpl delegate, final LoadedTypeDefinition superType, final LoadedTypeDefinition[] interfaces, final ArrayList<FieldElement> fields, final MethodElement[] methods, final MethodElement[] instanceMethods, final ConstructorElement[] ctors, final InitializerElement init, final NestedClassElement enclosingClass, final NestedClassElement[] enclosedClasses, DefinedTypeDefinition nestHost, DefinedTypeDefinition[] nestMembers) {
        this.delegate = delegate;
        this.superType = superType;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;
        this.instanceMethods = instanceMethods;
        this.ctors = ctors;
        this.init = init;
        this.enclosingClass = enclosingClass;
        this.enclosedClasses = enclosedClasses;
        this.nestHost = nestHost;
        this.nestMembers = nestMembers;
        int interfaceCnt = interfaces.length;
        InterfaceObjectType[] interfaceTypes = new InterfaceObjectType[interfaceCnt];
        for (int i = 0; i < interfaceCnt; i ++) {
            interfaceTypes[i] = interfaces[i].getInterfaceType();
        }
        if (isPrimitive()) {
            type = Primitive.getPrimitiveFor((BaseTypeDescriptor) getDescriptor()).getType();
        } else if (isInterface()) {
            type = getContext().getTypeSystem().generateInterfaceObjectType(delegate, List.of(interfaceTypes));
        } else {
            type = getContext().getTypeSystem().generateClassObjectType(delegate, superType == null ? null : superType.getClassType(), List.of(interfaceTypes));
        }

        /* Walk methods of interfaces looking for default methods */
        boolean buildDeclaresDefaultMethods = false;
        if (isInterface()) {
            for (MethodElement method : instanceMethods) {
                /* Only look at locally defined methods to determine if 
                 * this class has default methods 
                 */
                if (method.getEnclosingType().equals(delegate)) {
                    if (!method.isStatic() && !method.isAbstract()) {
                        buildDeclaresDefaultMethods = true;
                    }
                }
            }
        }
        declaresDefaultMethods = buildDeclaresDefaultMethods;

        /* For both classes & interfaces, they either inherit "hasDefaultMethods"
         * from their superclass or interfaces, or compute it (aka declaresDefaultMethods)
         */
        boolean buildHasDefaultMethods = declaresDefaultMethods;
        if (superType != null) { /* Object */
            buildHasDefaultMethods |= superType.hasDefaultMethods(); 
        }
        if (!buildHasDefaultMethods) {
            for (LoadedTypeDefinition ltd : interfaces) {
                buildHasDefaultMethods = ltd.hasDefaultMethods();
            }
        }
        hasDefaultMethods = buildHasDefaultMethods;
    }

    // delegates

    public DefinedTypeDefinition getDelegate() {
        return delegate;
    }

    public LoadedTypeDefinition load() {
        return this;
    }

    // local methods

    public ValueType getType() {
        return type;
    }

    public LoadedTypeDefinition getSuperClass() {
        return superType;
    }

    public LoadedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return interfaces[index];
    }

    public LoadedTypeDefinition[] getInterfaces() {
        return interfaces.clone();
    }

    public void forEachInterfaceFullImplementedSet(Consumer<LoadedTypeDefinition> function) {
        ArrayDeque<LoadedTypeDefinition> worklist = new ArrayDeque<>();
        LoadedTypeDefinition cur = this;
        while (cur != null) {
            Collections.addAll(worklist, cur.getInterfaces());
            cur = cur.getSuperClass();
        }
        if (this.isInterface()) {
            worklist.add(this);
        }
        // worklist contains all directly implemented interfaces in the super class chain
        HashSet<LoadedTypeDefinition> visited = new HashSet<>();
        while (!worklist.isEmpty()) {
            LoadedTypeDefinition ltd = worklist.pop();
            if (visited.add(ltd)) {
                function.accept(ltd);
                // Now add the interface's superinterface hierarchy to the worklist
                Collections.addAll(worklist, ltd.getInterfaces());
            }
        }
    }

    @Override
    public DefinedTypeDefinition getNestHost() {
        return nestHost;
    }

    @Override
    public DefinedTypeDefinition[] getNestMembers() {
        return nestMembers;
    }

    public MethodElement[] getInstanceMethods() { return instanceMethods; }

    public NestedClassElement getEnclosingNestedClass() {
        return enclosingClass;
    }

    public int getEnclosedNestedClassCount() {
        return enclosedClasses.length;
    }

    public NestedClassElement getEnclosedNestedClass(final int index) throws IndexOutOfBoundsException {
        return enclosedClasses[index];
    }

    public int getFieldCount() {
        return fields.size();
    }

    public FieldElement getField(final int index) {
        return fields.get(index);
    }

    public void injectField(final FieldElement field) {
        Assert.checkNotNullParam("field", field);
        if ((field.getModifiers() & ClassFile.I_ACC_NO_RESOLVE) == 0) {
            throw new IllegalArgumentException("Injected fields must be unresolvable");
        }
        fields.add(field);
    }

    public Value getInitialValue(FieldElement field) {
        return vmClass == null ? ((StaticFieldElement)field).getInitialValue() : vmClass.getValueForStaticField(field);
    }

    public int getMethodCount() {
        return methods.length;
    }

    public MethodElement getMethod(final int index) {
        return methods[index];
    }

    @Override
    public MethodElement expandSigPolyMethod(MethodElement original, MethodDescriptor descriptor) {
        if (original.isSignaturePolymorphic()) {
            // find or realize a copy of the signature-polymorphic method
            return getSigPolyMethods().computeIfAbsent(original, LoadedTypeDefinitionImpl::newMap).computeIfAbsent(descriptor, desc -> {
                MethodElement.Builder builder = MethodElement.builder(original.getName(), desc, original.getIndex());
                builder.setSignature(MethodSignature.synthesize(getContext(), desc));
                builder.setEnclosingType(original.getEnclosingType());
                builder.setMinimumLineNumber(original.getMinimumLineNumber());
                builder.setMaximumLineNumber(original.getMaximumLineNumber());
                builder.setModifiers(original.getModifiers() & ~ClassFile.I_ACC_SIGNATURE_POLYMORPHIC & ~ClassFile.ACC_FINAL & ~ClassFile.ACC_NATIVE | ClassFile.ACC_SYNTHETIC);
                MethodBodyFactory origFactory = original.getMethodBodyFactory();
                if (origFactory != null) {
                    builder.setMethodBodyFactory(origFactory, original.getMethodBodyFactoryIndex());
                }
                builder.addInvisibleAnnotations(original.getInvisibleAnnotations());
                builder.addVisibleAnnotations(original.getVisibleAnnotations());
                List<TypeDescriptor> parameterTypes = desc.getParameterTypes();
                int cnt = parameterTypes.size();
                List<ParameterElement> params = new ArrayList<>(cnt);
                for (int i = 0; i < cnt; i ++) {
                    ParameterElement.Builder pb = ParameterElement.builder("p" + i, parameterTypes.get(i), i);
                    pb.setEnclosingType(original.getEnclosingType());
                    pb.setTypeParameterContext(original);
                    pb.setSignature(TypeSignature.synthesize(getContext(), pb.getDescriptor()));
                    params.add(pb.build());
                }
                builder.setParameters(params);
                return builder.build();
            });
        }
        return original;
    }

    private static <K, V> Map<K, V> newMap(final Object ignored) {
        return new ConcurrentHashMap<>();
    }

    private Map<MethodElement, Map<MethodDescriptor, MethodElement>> getSigPolyMethods() {
        Map<MethodElement, Map<MethodDescriptor, MethodElement>> sigPolyMethods = this.sigPolyMethods;
        if (sigPolyMethods == null) {
            synchronized (this) {
                sigPolyMethods = this.sigPolyMethods;
                if (sigPolyMethods == null) {
                    this.sigPolyMethods = sigPolyMethods = new ConcurrentHashMap<>();
                }
            }
        }
        return sigPolyMethods;
    }

    @Override
    public <T> void forEachSigPolyInstanceMethod(T arg, BiConsumer<T, ? super InstanceMethodElement> consumer) {
        Map<MethodElement, Map<MethodDescriptor, MethodElement>> sigPolyMethods = this.sigPolyMethods;
        if (sigPolyMethods != null) {
            for (Map<MethodDescriptor, MethodElement> subMap : sigPolyMethods.values()) {
                for (MethodElement element : subMap.values()) {
                    if (element instanceof InstanceMethodElement ime) {
                        consumer.accept(arg, ime);
                    }
                }
            }
        }
    }

    @Override
    public <T> void forEachSigPolyMethod(T arg, BiConsumer<T, ? super MethodElement> consumer) {
        Map<MethodElement, Map<MethodDescriptor, MethodElement>> sigPolyMethods = this.sigPolyMethods;
        if (sigPolyMethods != null) {
            for (Map<MethodDescriptor, MethodElement> subMap : sigPolyMethods.values()) {
                for (MethodElement element : subMap.values()) {
                    consumer.accept(arg, element);
                }
            }
        }
    }

    public int getConstructorCount() {
        return ctors.length;
    }

    public ConstructorElement getConstructor(final int index) {
        return ctors[index];
    }

    public InitializerElement getInitializer() {
        return init;
    }

    // next stage

    public int getTypeId() {
        return typeId;
    }

    public int getMaximumSubtypeId() {
        return maximumSubtypeId;
    }

    public boolean isTypeIdValid() {
        return typeId != -1;
    }

    public void assignTypeId(int myTypeId) {
        // typeId shouldn't hae already been assigned
        Assert.assertTrue(typeId == -1);
        typeId = myTypeId;
    }

    public void assignMaximumSubtypeId(int subTypeId) {
        // maximumSubtypeId shouldn't hae already been assigned
        Assert.assertTrue(maximumSubtypeId == -1);
        maximumSubtypeId = subTypeId;
    }

    public boolean declaresDefaultMethods() {
        // only interfaces can declare default methods
        Assert.assertTrue(isInterface() || (declaresDefaultMethods == false));
        return declaresDefaultMethods;
    }

    public boolean hasDefaultMethods() {
        return hasDefaultMethods;
    }

    public VmClass getVmClass() {
        VmClass vmClass = this.vmClass;
        if (vmClass == null) {
            Vm vm = getContext().getCompilationContext().getVm();
            vmClass = vm.getClassLoaderForContext(getContext()).getOrDefineClass(this);
            setVmClass(vmClass);
        }
        return vmClass;
    }

    @Override
    public void setVmClass(VmClass vmClass) {
        VmClass res = (VmClass) vmClassHandle.compareAndExchange(this, null, vmClass);
        if (res != null && res != vmClass) {
            throw new IllegalStateException("VmClass already set for " + this);
        }
    }

    public LoadedTypeDefinition getEnclosingMethodClass() {
        LoadedTypeDefinition emc = this.enclosingMethodClass;
        if (emc == null) {
            String emcName = delegate.enclosingMethodClassName;
            if (emcName == null) {
                return null;
            }
            DefinedTypeDefinition definedType = getContext().findDefinedType(emcName);
            if (definedType == null) {
                return null;
            }
            this.enclosingMethodClass = emc = definedType.load();
        }
        return emc;
    }

    public MethodElement getEnclosingMethod() {
        MethodElement em = this.enclosingMethod;
        if (em == null) {
            LoadedTypeDefinition emc = getEnclosingMethodClass();
            if (emc == null) {
                return null;
            }
            String emName = delegate.enclosingMethodName;
            MethodDescriptor emDesc = delegate.enclosingMethodDesc;
            if (emName != null && emDesc != null) {
                int emIdx = emc.findMethodIndex(emName, emDesc);
                if (emIdx != -1) {
                    this.enclosingMethod = em = emc.getMethod(emIdx);
                }
            }
        }
        return em;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
