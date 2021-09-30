package org.qbicc.type.definition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.Value;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;

/**
 *
 */
final class LoadedTypeDefinitionImpl extends DelegatingDefinedTypeDefinition implements LoadedTypeDefinition {

    private final ObjectType type;
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
    private int typeId = -1;
    private int maximumSubtypeId = -1;
    private final boolean hasDefaultMethods;
    private final boolean declaresDefaultMethods;
    private volatile VmClass vmClass;

    LoadedTypeDefinitionImpl(final DefinedTypeDefinitionImpl delegate, final LoadedTypeDefinition superType, final LoadedTypeDefinition[] interfaces, final ArrayList<FieldElement> fields, final MethodElement[] methods, final MethodElement[] instanceMethods, final ConstructorElement[] ctors, final InitializerElement init, final NestedClassElement enclosingClass, final NestedClassElement[] enclosedClasses) {
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
        int interfaceCnt = interfaces.length;
        InterfaceObjectType[] interfaceTypes = new InterfaceObjectType[interfaceCnt];
        for (int i = 0; i < interfaceCnt; i ++) {
            interfaceTypes[i] = interfaces[i].getInterfaceType();
        }
        if (isInterface()) {
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

    public ObjectType getType() {
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
        return vmClass == null ? field.getInitialValue() : vmClass.getValueForStaticField(field);
    }

    public int getMethodCount() {
        return methods.length;
    }

    public MethodElement getMethod(final int index) {
        return methods[index];
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
            Vm vm = Vm.requireCurrent();
            vmClass = this.vmClass = vm.getClassLoaderForContext(getContext()).getOrDefineClass(this);
        }
        return vmClass;
    }
}
