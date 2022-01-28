package org.qbicc.type.definition;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.qbicc.context.ClassContext;
import org.qbicc.context.Location;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.classfile.BootstrapMethod;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.definition.element.ParameterElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.ClassSignature;
import io.smallrye.common.constraint.Assert;
import org.qbicc.type.generic.MethodSignature;

/**
 *
 */
final class DefinedTypeDefinitionImpl implements DefinedTypeDefinition {
    private final ClassContext context;
    private final String simpleName;
    private final String internalName;
    private final String superClassName;
    private final int modifiers;
    private final String[] interfaceNames;
    private final ClassTypeDescriptor descriptor;
    private final ClassSignature signature;
    private final String[] methodNames;
    private final MethodDescriptor[] methodDescriptors;
    private final MethodResolver[] methodResolvers;
    private final int[] methodIndexes;
    private final String[] fieldNames;
    private final TypeDescriptor[] fieldDescriptors;
    private final FieldResolver[] fieldResolvers;
    private final int[] fieldIndexes;
    private final MethodDescriptor[] constructorDescriptors;
    private final ConstructorResolver[] constructorResolvers;
    private final int[] constructorIndexes;
    // this is the whole-class initializer
    private final InitializerResolver initializerResolver;
    private final int initializerIndex;
    private final List<Annotation> visibleAnnotations;
    private final List<Annotation> invisibleAnnotations;
    private final TypeAnnotationList visibleTypeAnnotations;
    private final TypeAnnotationList invisibleTypeAnnotations;
    private final List<BootstrapMethod> bootstrapMethods;
    private final EnclosingClassResolver enclosingClassResolver;
    private final int enclosingClassResolverIndex;
    private final String enclosingClassName;
    private final EnclosedClassResolver[] enclosedClassResolvers;
    private final int[] enclosedClassResolverIndexes;
    private final DefinedTypeDefinition superClass;
    final String enclosingMethodClassName;
    final String enclosingMethodName;
    final MethodDescriptor enclosingMethodDesc;

    private volatile DefinedTypeDefinition loaded;

    private static final String[] NO_STRINGS = new String[0];
    private static final TypeDescriptor[] NO_DESCRIPTORS = new TypeDescriptor[0];
    private static final MethodDescriptor[] NO_METHOD_DESCRIPTORS = new MethodDescriptor[0];
    private static final int[] NO_INTS = new int[0];
    private static final MethodResolver[] NO_METHODS = new MethodResolver[0];
    private static final FieldResolver[] NO_FIELDS = new FieldResolver[0];
    private static final ConstructorResolver[] NO_CONSTRUCTORS = new ConstructorResolver[0];
    private static final EnclosedClassResolver[] NO_ENCLOSED = new EnclosedClassResolver[0];

    DefinedTypeDefinitionImpl(final BuilderImpl builder) {
        this.context = builder.context;
        this.internalName = Assert.checkNotNullParam("builder.internalName", builder.internalName);
        this.superClassName = builder.superClassName;
        this.superClass = builder.superClass;
        String simpleName = builder.simpleName;
        if (simpleName == null) {
            int idx = internalName.lastIndexOf('/');
            this.simpleName = idx == -1 ? internalName : internalName.substring(idx + 1);
        } else {
            this.simpleName = simpleName;
        }
        this.modifiers = builder.modifiers;
        int interfaceCount = builder.interfaceCount;
        this.interfaceNames = interfaceCount == 0 ? NO_STRINGS : Arrays.copyOf(builder.interfaceNames, interfaceCount);
        int methodCount = builder.methodCount;
        this.descriptor = Assert.checkNotNullParam("builder.descriptor", builder.descriptor);
        this.signature = Assert.checkNotNullParam("builder.signature", builder.signature);
        this.methodNames = methodCount == 0 ? NO_STRINGS : Arrays.copyOf(builder.methodNames, methodCount);
        this.methodDescriptors = methodCount == 0 ? NO_METHOD_DESCRIPTORS : Arrays.copyOf(builder.methodDescriptors, methodCount);
        this.methodResolvers = methodCount == 0 ? NO_METHODS : Arrays.copyOf(builder.methodResolvers, methodCount);
        this.methodIndexes = methodCount == 0 ? NO_INTS : Arrays.copyOf(builder.methodIndexes, methodCount);
        int fieldCount = builder.fieldCount;
        this.fieldNames = fieldCount == 0 ? NO_STRINGS : Arrays.copyOf(builder.fieldNames, fieldCount);
        this.fieldDescriptors = fieldCount == 0 ? NO_DESCRIPTORS : Arrays.copyOf(builder.fieldDescriptors, fieldCount);
        this.fieldResolvers = fieldCount == 0 ? NO_FIELDS : Arrays.copyOf(builder.fieldResolvers, fieldCount);
        this.fieldIndexes = fieldCount == 0 ? NO_INTS : Arrays.copyOf(builder.fieldIndexes, fieldCount);
        int constructorCount = builder.constructorCount;
        this.constructorDescriptors = constructorCount == 0 ? NO_METHOD_DESCRIPTORS : Arrays.copyOf(builder.constructorDescriptors, constructorCount);
        this.constructorResolvers = constructorCount == 0 ? NO_CONSTRUCTORS : Arrays.copyOf(builder.constructorResolvers, constructorCount);
        this.constructorIndexes = constructorCount == 0 ? NO_INTS : Arrays.copyOf(builder.constructorIndexes, constructorCount);
        this.visibleAnnotations = builder.visibleAnnotations;
        this.invisibleAnnotations = builder.invisibleAnnotations;
        this.visibleTypeAnnotations = builder.visibleTypeAnnotations;
        this.invisibleTypeAnnotations = builder.invisibleTypeAnnotations;
        this.bootstrapMethods = builder.bootstrapMethods;
        this.initializerResolver = Assert.checkNotNullParam("builder.initializerResolver", builder.initializerResolver);
        this.initializerIndex = builder.initializerIndex;
        this.enclosingClassResolver = builder.enclosingClassResolver;
        this.enclosingClassResolverIndex = builder.enclosingClassResolverIndex;
        this.enclosingClassName = builder.enclosingClassInternalName;
        int enclosedClassCount = builder.enclosedClassCount;
        this.enclosedClassResolvers = enclosedClassCount == 0 ? NO_ENCLOSED : Arrays.copyOf(builder.enclosedClassResolvers, enclosedClassCount);
        this.enclosedClassResolverIndexes = enclosedClassCount == 0 ? NO_INTS : Arrays.copyOf(builder.enclosedClassResolverIndexes, enclosedClassCount);
        enclosingMethodClassName = builder.enclosingMethodClassName;
        enclosingMethodName = builder.enclosingMethodName;
        enclosingMethodDesc = builder.enclosingMethodDesc;
    }

    public ClassContext getContext() {
        return context;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public boolean internalNameEquals(final String internalName) {
        return this.internalName.equals(Assert.checkNotNullParam("internalName", internalName));
    }

    public boolean internalPackageAndNameEquals(final String intPackageName, final String className) {
        int classLen = className.length();
        int pkgLen = intPackageName.length();
        return internalName.length() == pkgLen + classLen + 1
            && intPackageName.regionMatches(0, internalName, 0, pkgLen)
            && internalName.charAt(pkgLen) == '/'
            && className.regionMatches(0, internalName, pkgLen + 1, classLen);
    }

    public ClassTypeDescriptor getDescriptor() {
        return descriptor;
    }

    public ClassSignature getSignature() {
        return signature;
    }

    public int getModifiers() {
        return modifiers;
    }

    public String getEnclosingClassInternalName() {
        return enclosingClassName;
    }

    public String getSuperClassInternalName() {
        return superClassName;
    }

    public boolean superClassInternalNameEquals(final String internalName) {
        return Objects.equals(this.superClassName, internalName);
    }

    public int getInterfaceCount() {
        return interfaceNames.length;
    }

    public String getInterfaceInternalName(final int index) throws IndexOutOfBoundsException {
        return interfaceNames[index];
    }

    public boolean interfaceInternalNameEquals(final int index, final String internalName) throws IndexOutOfBoundsException {
        return getInterfaceInternalName(index).equals(internalName);
    }

    public LoadedTypeDefinition load() throws VerifyFailedException {
        DefinedTypeDefinition loaded = this.loaded;
        if (loaded != null) {
            return loaded.load();
        }
        LoadedTypeDefinition superType;
        if (superClass != null) {
            superType = superClass.load();
        } else if (superClassName != null) {
            DefinedTypeDefinition definedSuperType = context.findDefinedType(superClassName);
            if (definedSuperType == null) {
                throw new VerifyFailedException("Failed to load super class " + superClassName);
            }
            superType = definedSuperType.load();
        } else {
            superType = null;
        }
        int cnt = getInterfaceCount();
        LoadedTypeDefinition[] interfaces = new LoadedTypeDefinition[cnt];
        for (int i = 0; i < cnt; i ++) {
            interfaces[i] = context.findDefinedType(getInterfaceInternalName(i)).load();
        }
        // one more try before taking a lock
        loaded = this.loaded;
        if (loaded != null) {
            return loaded.load();
        }
        synchronized (this) {
            loaded = this.loaded;
            if (loaded != null) {
                return loaded.load();
            }
            cnt = getFieldCount();
            ArrayList<FieldElement> fields = new ArrayList<>(cnt);
            for (int i = 0; i < cnt; i ++) {
                fields.add(fieldResolvers[i].resolveField(fieldIndexes[i], this, FieldElement.builder(fieldNames[i], fieldDescriptors[i], i)));
            }
            cnt = getMethodCount();
            MethodElement[] methods;
            if (isInterface()) {
                methods = cnt == 0 ? MethodElement.NO_METHODS : new MethodElement[cnt];
                for (int i = 0; i < cnt; i ++) {
                    methods[i] = methodResolvers[i].resolveMethod(methodIndexes[i], this, MethodElement.builder(methodNames[i], methodDescriptors[i], i));
                }
            } else {
                List<MethodElement> methodsList = new ArrayList<>(cnt + (cnt >> 1)); // 1.5x size
                for (int i = 0; i < cnt; i ++) {
                    methodsList.add(methodResolvers[i].resolveMethod(methodIndexes[i], this, MethodElement.builder(methodNames[i], methodDescriptors[i], i)));
                }
                // now add methods for any maximally-specific interface methods that are not implemented by this class
                // - but, if the method is default, let's copy it in, body and all
                HashMap<String, HashSet<MethodDescriptor>> visited = new HashMap<>();
                // populate the set of methods that we already have implementations for
                for (MethodElement method : methodsList) {
                    addMethodToVisitedSet(method, visited);
                }
                addMethodsToVisitedSet(superType, visited);
                // now the visited set contains all methods we do not need to implement
                // next we find the set of methods that we're missing
                for (int depth = 0;; depth ++) {
                    // at each depth stage, create a new to-add set
                    HashMap<String, HashMap<MethodDescriptor, List<MethodElement>>> toAdd = new HashMap<>();
                    if (findInterfaceImplementations(interfaces, visited, toAdd, depth) == 0) {
                        break;
                    }
                    // we may have found methods to add
                    for (Map.Entry<String, HashMap<MethodDescriptor, List<MethodElement>>> entry : toAdd.entrySet()) {
                        String name = entry.getKey();
                        HashMap<MethodDescriptor, List<MethodElement>> subMap = entry.getValue();
                        nextMethod: for (Map.Entry<MethodDescriptor, List<MethodElement>> entry2 : subMap.entrySet()) {
                            MethodDescriptor descriptor = entry2.getKey();
                            List<MethodElement> elementList = entry2.getValue();
                            MethodElement defaultMethod = null;
                            MethodElement oneOfTheMethods = null;
                            for (MethodElement element : elementList) {
                                if (oneOfTheMethods == null) {
                                    oneOfTheMethods = element;
                                }
                                if (! element.isAbstract()) {
                                    // found a default method
                                    if (defaultMethod == null) {
                                        defaultMethod = element;
                                    } else {
                                        // conflict method! Synthesize a method that throws ICCE
                                        MethodElement.Builder builder = MethodElement.builder(name, descriptor, methodsList.size());
                                        builder.setEnclosingType(this);
                                        // inheritable interface methods are public
                                        builder.setModifiers(ClassFile.ACC_PUBLIC);
                                        builder.setInvisibleAnnotations(List.of());
                                        builder.setVisibleAnnotations(List.of());
                                        builder.setSignature(MethodSignature.synthesize(context, descriptor));
                                        // synthesize parameter objects
                                        builder.setParameters(copyParametersFrom(element));
                                        builder.setMethodBodyFactory((index, e) -> {
                                            LoadedTypeDefinition ltd = e.getEnclosingType().load();
                                            BasicBlockBuilder bbb = context.newBasicBlockBuilder(e);
                                            ReferenceType thisType = ltd.getType().getReference();
                                            ParameterValue thisValue = bbb.parameter(thisType, "this", 0);
                                            FunctionType type = e.getType();
                                            int pcnt = type.getParameterCount();
                                            List<ParameterValue> paramValues = new ArrayList<>(pcnt);
                                            for (int i = 0; i < pcnt; i ++) {
                                                paramValues.add(bbb.parameter(type.getParameterType(i), "p", i));
                                            }
                                            bbb.startMethod(paramValues);
                                            // build the entry block
                                            BlockLabel entryLabel = new BlockLabel();
                                            bbb.begin(entryLabel);
                                            ClassContext bc = context.getCompilationContext().getBootstrapClassContext();
                                            LoadedTypeDefinition vmHelpers = bc.findDefinedType("org/qbicc/runtime/main/VMHelpers").load();
                                            MethodElement icce = vmHelpers.resolveMethodElementExact("raiseIncompatibleClassChangeError", MethodDescriptor.synthesize(bc, BaseTypeDescriptor.V, List.of()));
                                            BasicBlock entryBlock = bbb.callNoReturn(bbb.staticMethod(icce, icce.getDescriptor(), icce.getType()), List.of());
                                            bbb.finish();
                                            Schedule schedule = Schedule.forMethod(entryBlock);
                                            return MethodBody.of(entryBlock, schedule, thisValue, paramValues);
                                        }, 0);
                                        methodsList.add(builder.build());
                                        continue nextMethod;
                                    }
                                }
                            }
                            assert oneOfTheMethods != null;
                            // non-conflict method
                            MethodElement.Builder builder = MethodElement.builder(name, descriptor, methodsList.size());
                            builder.setEnclosingType(this);
                            builder.setInvisibleAnnotations(List.of());
                            builder.setVisibleAnnotations(List.of());
                            builder.setSignature(MethodSignature.synthesize(context, descriptor));
                            builder.setParameters(copyParametersFrom(oneOfTheMethods));
                            if (defaultMethod == null) {
                                // add an abstract method
                                // inheritable interface methods are public
                                builder.setModifiers(ClassFile.ACC_PUBLIC | ClassFile.ACC_ABSTRACT);
                            } else {
                                // add the default method
                                // inheritable interface methods are public
                                builder.setModifiers(ClassFile.ACC_PUBLIC | ClassFile.I_ACC_HIDDEN);
                                // synthesize a tail-calling exact delegation to the default method
                                MethodElement finalDefaultMethod = defaultMethod;
                                builder.setMethodBodyFactory((index, e) -> {
                                    LoadedTypeDefinition ltd = e.getEnclosingType().load();
                                    BasicBlockBuilder bbb = context.newBasicBlockBuilder(e);
                                    ReferenceType thisType = ltd.getType().getReference();
                                    ParameterValue thisValue = bbb.parameter(thisType, "this", 0);
                                    FunctionType type = e.getType();
                                    int pcnt = type.getParameterCount();
                                    List<ParameterValue> paramValues = new ArrayList<>(pcnt);
                                    for (int i = 0; i < pcnt; i ++) {
                                        paramValues.add(bbb.parameter(type.getParameterType(i), "p", i));
                                    }
                                    bbb.startMethod(paramValues);
                                    // build the entry block
                                    BlockLabel entryLabel = new BlockLabel();
                                    bbb.begin(entryLabel);
                                    // just cast the list because it's fine; todo: maybe this method should accept List<? extends Value>
                                    //noinspection unchecked,rawtypes
                                    BasicBlock entryBlock = bbb.tailCall(bbb.exactMethodOf(thisValue, finalDefaultMethod, finalDefaultMethod.getDescriptor(), finalDefaultMethod.getType()), (List<Value>) (List) paramValues);
                                    bbb.finish();
                                    Schedule schedule = Schedule.forMethod(entryBlock);
                                    return MethodBody.of(entryBlock, schedule, thisValue, paramValues);
                                }, 0);
                            }
                            methodsList.add(builder.build());
                        }
                    }
                }
                methods = methodsList.toArray(MethodElement[]::new);
            }
            cnt = getConstructorCount();
            ConstructorElement[] ctors = cnt == 0 ? ConstructorElement.NO_CONSTRUCTORS : new ConstructorElement[cnt];
            for (int i = 0; i < cnt; i ++) {
                ctors[i] = constructorResolvers[i].resolveConstructor(constructorIndexes[i], this, ConstructorElement.builder(constructorDescriptors[i], i));
            }
            InitializerElement init = initializerResolver.resolveInitializer(initializerIndex, this, InitializerElement.builder());
            NestedClassElement enclosingClass = enclosingClassResolver == null ? null : enclosingClassResolver.resolveEnclosingNestedClass(enclosingClassResolverIndex, this, NestedClassElement.builder(0));
            NestedClassElement[] enclosedClasses = resolveEnclosedClasses(enclosedClassResolvers, enclosedClassResolverIndexes, 0, 0);

            // Construct instanceMethods -- the ordered list of all instance methods inherited and directly implemented.
            ArrayList<MethodElement> instanceMethods = new ArrayList<>();
            if (superType != null && !isInterface()) {
                // (i) all instance methods of my superclass
                instanceMethods.addAll(List.of(superType.getInstanceMethods()));
            }
            for (LoadedTypeDefinition i: interfaces) {
                outer: for (MethodElement im: i.getInstanceMethods()) {
                    for (MethodElement already : instanceMethods) {
                        if (already.getName().equals(im.getName()) && already.getDescriptor().equals(im.getDescriptor())) {
                            continue outer;
                        }
                    }
                    // (ii) abstract instance methods implied by an implemented/extended interfaces
                    instanceMethods.add(im);
                }
            }
            outer: for (MethodElement dm: methods) {
                if (!dm.isStatic()) {
                    for (int i=0; i<instanceMethods.size(); i++) {
                        if (instanceMethods.get(i).getName().equals(dm.getName()) && instanceMethods.get(i).getDescriptor().equals(dm.getDescriptor())) {
                            // override inherited method
                            instanceMethods.set(i, dm);
                            continue outer;
                        }
                    }
                    // (iii) instance method newly defined in this class/interface
                    instanceMethods.add(dm);
                }
            }
            MethodElement[] instMethods = instanceMethods.toArray(new MethodElement[instanceMethods.size()]);
            try {
                loaded = new LoadedTypeDefinitionImpl(this, superType, interfaces, fields, methods, instMethods, ctors, init, enclosingClass, enclosedClasses);
            } catch (VerifyFailedException e) {
                this.loaded = new VerificationFailedDefinitionImpl(this, e.getMessage(), e.getCause());
                throw e;
            }
            // replace in the map *first*, *then* replace our local ref
            // definingLoader.replaceTypeDefinition(name, this, verified);
            this.loaded = loaded;
            return loaded.load();
        }
    }

    private List<ParameterElement> copyParametersFrom(final MethodElement element) {
        List<ParameterElement> parameters = new ArrayList<>(element.getParameters().size());
        for (ParameterElement original : element.getParameters()) {
            ParameterElement.Builder paramBuilder = ParameterElement.builder(original);
            paramBuilder.setEnclosingType(this);
            paramBuilder.setSourceFileName(null);
            parameters.add(paramBuilder.build());
        }
        return parameters;
    }

    private void addMethodsToVisitedSet(final LoadedTypeDefinition ltd, final HashMap<String, HashSet<MethodDescriptor>> visited) {
        if (ltd == null) {
            return;
        }
        int cnt = ltd.getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            addMethodToVisitedSet(ltd.getMethod(i), visited);
        }
        addMethodsToVisitedSet(ltd.getSuperClass(), visited);
    }

    /**
     * Add one method to the visited set.
     * @param method the method to add
     * @param visited the visited set
     * @return {@code true} if the method is not static or private and was not previously in the set, or {@code false} if no new method was added
     */
    private boolean addMethodToVisitedSet(final MethodElement method, final HashMap<String, HashSet<MethodDescriptor>> visited) {
        if (method.isStatic() || method.isPrivate()) {
            return false;
        } else {
            return visited.computeIfAbsent(method.getName(), DefinedTypeDefinitionImpl::newSet).add(method.getDescriptor());
        }
    }

    private int findInterfaceImplementations(LoadedTypeDefinition[] interfaces, HashMap<String, HashSet<MethodDescriptor>> visited, HashMap<String, HashMap<MethodDescriptor, List<MethodElement>>> toAdd, int depth) {
        int processed = 0;
        for (LoadedTypeDefinition interface_ : interfaces) {
            processed += findInterfaceImplementations(interface_, visited, toAdd, depth);
        }
        return processed;
    }

    private int findInterfaceImplementations(LoadedTypeDefinition currentInterface, HashMap<String, HashSet<MethodDescriptor>> visited, HashMap<String, HashMap<MethodDescriptor, List<MethodElement>>> toAdd, int depth) {
        if (depth > 0) {
            return findInterfaceImplementations(currentInterface.getInterfaces(), visited, toAdd, depth - 1);
        } else {
            int cnt = currentInterface.getMethodCount();
            search: for (int i = 0; i < cnt; i ++) {
                MethodElement method = currentInterface.getMethod(i);
                if (addMethodToVisitedSet(method, visited)) {
                    // we didn't have this one before
                    toAdd.computeIfAbsent(method.getName(), DefinedTypeDefinitionImpl::newMap).computeIfAbsent(method.getDescriptor(), DefinedTypeDefinitionImpl::newList).add(method);
                } else {
                    // check to see whether we need to add this one to our level
                    HashMap<MethodDescriptor, List<MethodElement>> map1 = toAdd.get(method.getName());
                    if (map1 != null) {
                        List<MethodElement> list = map1.get(method.getDescriptor());
                        if (list != null) {
                            // first make sure that this method element does not override any of the already-known method elements
                            ListIterator<MethodElement> iter = list.listIterator();
                            while (iter.hasNext()) {
                                MethodElement current = iter.next();
                                if (method.overrides(current)) {
                                    // remove old candidate
                                    iter.remove();
                                } else if (current.overrides(method)) {
                                    // no action needed since a better choice already exists
                                    continue search;
                                }
                            }
                            // we have another impl option for this
                            iter.add(method);
                        }
                    }
                }
            }
            return 1;
        }
    }

    private static <E> HashSet<E> newSet(final Object ignored) {
        return new HashSet<>(4);
    }

    private static <K, V> HashMap<K, V> newMap(final Object ignored) {
        return new HashMap<>(4);
    }

    private static <E> ArrayList<E> newList(final Object ignored) {
        return new ArrayList<>(3);
    }

    private NestedClassElement[] resolveEnclosedClasses(final EnclosedClassResolver[] resolvers, final int[] indexes, final int inIdx, final int outIdx) {
        int maxInIdx = resolvers.length;
        if (inIdx == maxInIdx) {
            return outIdx == 0 ? NestedClassElement.NO_NESTED_CLASSES : new NestedClassElement[outIdx];
        }
        NestedClassElement resolved = resolvers[inIdx].resolveEnclosedNestedClass(indexes[inIdx], this, NestedClassElement.builder(inIdx));
        if (resolved != null) {
            NestedClassElement[] array = resolveEnclosedClasses(resolvers, indexes, inIdx + 1, outIdx + 1);
            array[outIdx] = resolved;
            return array;
        } else {
            return resolveEnclosedClasses(resolvers, indexes, inIdx + 1, outIdx);
        }
    }

    public int getFieldCount() {
        return fieldResolvers.length;
    }

    public int getMethodCount() {
        return methodResolvers.length;
    }

    public int getConstructorCount() {
        return constructorResolvers.length;
    }

    public List<Annotation> getVisibleAnnotations() {
        return visibleAnnotations;
    }

    public List<Annotation> getInvisibleAnnotations() {
        return invisibleAnnotations;
    }

    public TypeAnnotationList getVisibleTypeAnnotations() {
        return visibleTypeAnnotations;
    }

    public TypeAnnotationList getInvisibleTypeAnnotations() {
        return invisibleTypeAnnotations;
    }

    public List<BootstrapMethod> getBootstrapMethods() {
        return bootstrapMethods;
    }

    public BootstrapMethod getBootstrapMethod(final int index) {
        return bootstrapMethods.get(index);
    }

    public boolean hasSuperClass() {
        return superClassName != null;
    }

    // internal

    static class BuilderImpl implements Builder {
        ClassContext context;
        String internalName;
        String superClassName = "java/lang/Object";
        int modifiers = ClassFile.ACC_SUPER;
        int interfaceCount;
        String[] interfaceNames = NO_STRINGS;
        ClassTypeDescriptor descriptor;
        ClassSignature signature;
        int methodCount;
        MethodResolver[] methodResolvers = NO_METHODS;
        int[] methodIndexes = NO_INTS;
        String[] methodNames = NO_STRINGS;
        MethodDescriptor[] methodDescriptors = NO_METHOD_DESCRIPTORS;
        int fieldCount;
        FieldResolver[] fieldResolvers = NO_FIELDS;
        int[] fieldIndexes = NO_INTS;
        String[] fieldNames = NO_STRINGS;
        TypeDescriptor[] fieldDescriptors = NO_DESCRIPTORS;
        int constructorCount;
        ConstructorResolver[] constructorResolvers = NO_CONSTRUCTORS;
        int[] constructorIndexes = NO_INTS;
        MethodDescriptor[] constructorDescriptors = NO_METHOD_DESCRIPTORS;
        InitializerResolver initializerResolver;
        int initializerIndex;
        List<Annotation> visibleAnnotations = List.of();
        List<Annotation> invisibleAnnotations = List.of();
        TypeAnnotationList visibleTypeAnnotations = TypeAnnotationList.empty();
        TypeAnnotationList invisibleTypeAnnotations = TypeAnnotationList.empty();
        List<BootstrapMethod> bootstrapMethods = List.of();
        String simpleName;
        EnclosingClassResolver enclosingClassResolver;
        String enclosingClassInternalName;
        int enclosingClassResolverIndex;
        int enclosedClassCount;
        EnclosedClassResolver[] enclosedClassResolvers = NO_ENCLOSED;
        int[] enclosedClassResolverIndexes = NO_INTS;
        DefinedTypeDefinition superClass;
        String enclosingMethodClassName;
        String enclosingMethodName;
        MethodDescriptor enclosingMethodDesc;

        public void setContext(final ClassContext context) {
            this.context = context;
        }

        public void setInitializer(final InitializerResolver resolver, final int index) {
            this.initializerResolver = Assert.checkNotNullParam("resolver", resolver);
            this.initializerIndex = index;
        }

        public void expectFieldCount(final int count) {
            Assert.checkMinimumParameter("count", 0, count);
            if (count == 0) {
                return;
            }
            FieldResolver[] fieldResolvers = this.fieldResolvers;
            if (fieldResolvers == null) {
                this.fieldResolvers = new FieldResolver[count];
                this.fieldIndexes = new int[count];
                this.fieldNames = new String[count];
                this.fieldDescriptors = new TypeDescriptor[count];
            } else if (fieldResolvers.length < count) {
                this.fieldResolvers = Arrays.copyOf(fieldResolvers, count);
                this.fieldIndexes = Arrays.copyOf(fieldIndexes, count);
                this.fieldNames = Arrays.copyOf(fieldNames, count);
                this.fieldDescriptors = Arrays.copyOf(fieldDescriptors, count);
            }
        }

        public void addField(final FieldResolver resolver, final int index, String name, TypeDescriptor descriptor) {
            Assert.checkNotNullParam("resolver", resolver);
            FieldResolver[] fieldResolvers = this.fieldResolvers;
            int[] fieldIndexes = this.fieldIndexes;
            String[] fieldNames = this.fieldNames;
            TypeDescriptor[] fieldDescriptors = this.fieldDescriptors;
            int len = fieldResolvers.length;
            int fieldCount = this.fieldCount;
            if (fieldCount == len) {
                // just grow it
                int newSize = len == 0 ? 4 : len << 1;
                fieldResolvers = this.fieldResolvers = Arrays.copyOf(fieldResolvers, newSize);
                fieldIndexes = this.fieldIndexes = Arrays.copyOf(fieldIndexes, newSize);
                fieldNames = this.fieldNames = Arrays.copyOf(fieldNames, newSize);
                fieldDescriptors = this.fieldDescriptors = Arrays.copyOf(fieldDescriptors, newSize);
            }
            fieldResolvers[fieldCount] = resolver;
            fieldIndexes[fieldCount] = index;
            fieldNames[fieldCount] = name;
            fieldDescriptors[fieldCount] = descriptor;
            this.fieldCount = fieldCount + 1;
        }

        public void expectMethodCount(final int count) {
            Assert.checkMinimumParameter("count", 0, count);
            if (count == 0) {
                return;
            }
            MethodResolver[] methodResolvers = this.methodResolvers;
            if (methodResolvers == null) {
                this.methodResolvers = new MethodResolver[count];
                this.methodIndexes = new int[count];
                this.methodNames = new String[count];
                this.methodDescriptors = new MethodDescriptor[count];
            } else if (methodResolvers.length < count) {
                this.methodResolvers = Arrays.copyOf(methodResolvers, count);
                this.methodIndexes = Arrays.copyOf(methodIndexes, count);
                this.methodNames = Arrays.copyOf(methodNames, count);
                this.methodDescriptors = Arrays.copyOf(methodDescriptors, count);
            }
        }

        public void addMethod(final MethodResolver resolver, final int index, String name, MethodDescriptor descriptor) {
            Assert.checkNotNullParam("resolver", resolver);
            MethodResolver[] methodResolvers = this.methodResolvers;
            int[] methodIndexes = this.methodIndexes;
            String[] methodNames = this.methodNames;
            MethodDescriptor[] methodDescriptors = this.methodDescriptors;
            int len = methodResolvers.length;
            int methodCount = this.methodCount;
            if (methodCount == len) {
                // just grow it
                int newSize = len == 0 ? 4 : len << 1;
                methodResolvers = this.methodResolvers = Arrays.copyOf(methodResolvers, newSize);
                methodIndexes = this.methodIndexes = Arrays.copyOf(methodIndexes, newSize);
                methodNames = this.methodNames = Arrays.copyOf(methodNames, newSize);
                methodDescriptors = this.methodDescriptors = Arrays.copyOf(methodDescriptors, newSize);
            }
            methodResolvers[methodCount] = resolver;
            methodIndexes[methodCount] = index;
            methodNames[methodCount] = name;
            methodDescriptors[methodCount] = descriptor;
            this.methodCount = methodCount + 1;
        }

        public void expectConstructorCount(final int count) {
            Assert.checkMinimumParameter("count", 0, count);
            if (count == 0) {
                return;
            }
            ConstructorResolver[] constructorResolvers = this.constructorResolvers;
            if (constructorResolvers == null) {
                this.constructorResolvers = new ConstructorResolver[count];
                this.constructorIndexes = new int[count];
                this.constructorDescriptors = new MethodDescriptor[count];
            } else if (constructorResolvers.length < count) {
                this.constructorResolvers = Arrays.copyOf(constructorResolvers, count);
                this.constructorIndexes = Arrays.copyOf(constructorIndexes, count);
                this.constructorDescriptors = Arrays.copyOf(constructorDescriptors, count);
            }
        }

        public void addConstructor(final ConstructorResolver resolver, final int index, MethodDescriptor descriptor) {
            Assert.checkNotNullParam("resolver", resolver);
            ConstructorResolver[] constructorResolvers = this.constructorResolvers;
            int[] constructorIndexes = this.constructorIndexes;
            MethodDescriptor[] constructorDescriptors = this.constructorDescriptors;
            int len = constructorResolvers.length;
            int constructorCount = this.constructorCount;
            if (constructorCount == len) {
                // just grow it
                int newSize = len == 0 ? 4 : len << 1;
                constructorResolvers = this.constructorResolvers = Arrays.copyOf(constructorResolvers, newSize);
                constructorIndexes = this.constructorIndexes = Arrays.copyOf(constructorIndexes, newSize);
                constructorDescriptors = this.constructorDescriptors = Arrays.copyOf(constructorDescriptors, newSize);
            }
            constructorResolvers[constructorCount] = resolver;
            constructorIndexes[constructorCount] = index;
            constructorDescriptors[constructorCount] = descriptor;
            this.constructorCount = constructorCount + 1;
        }

        public void setSimpleName(final String simpleName) {
            Assert.checkNotNullParam("simpleName", simpleName);
            this.simpleName = simpleName;
        }

        public void setEnclosingClass(final String internalName, final EnclosingClassResolver resolver, final int index) {
            Assert.checkNotNullParam("internalName", internalName);
            Assert.checkNotNullParam("resolver", resolver);
            this.enclosingClassResolver = resolver;
            this.enclosingClassResolverIndex = index;
            this.enclosingClassInternalName = internalName;
        }

        public void addEnclosedClass(final EnclosedClassResolver resolver, final int index) {
            Assert.checkNotNullParam("resolver", resolver);
            EnclosedClassResolver[] enclosedClassResolvers = this.enclosedClassResolvers;
            int[] enclosedClassIndexes = this.enclosedClassResolverIndexes;
            int len = enclosedClassResolvers.length;
            int enclosedClassCount = this.enclosedClassCount;
            if (enclosedClassCount == len) {
                // just grow it
                int newSize = len == 0 ? 4 : len << 1;
                enclosedClassResolvers = this.enclosedClassResolvers = Arrays.copyOf(enclosedClassResolvers, newSize);
                enclosedClassIndexes = this.enclosedClassResolverIndexes = Arrays.copyOf(enclosedClassIndexes, newSize);
            }
            enclosedClassResolvers[enclosedClassCount] = resolver;
            enclosedClassIndexes[enclosedClassCount] = index;
            this.enclosedClassCount = enclosedClassCount + 1;
        }

        public void setEnclosingMethod(final String classInternalName, final String methodName, final MethodDescriptor methodType) {
            Assert.checkNotNullParam("classInternalName", classInternalName);
            this.enclosingMethodClassName = classInternalName;
            this.enclosingMethodName = methodName;
            this.enclosingMethodDesc = methodType;
        }

        public void expectInterfaceNameCount(final int count) {
            Assert.checkMinimumParameter("count", 0, count);
            if (count == 0) {
                return;
            }
            String[] interfaceNames = this.interfaceNames;
            if (interfaceNames == null) {
                this.interfaceNames = new String[count];
            } else if (interfaceNames.length < count) {
                this.interfaceNames = Arrays.copyOf(interfaceNames, count);
            }
        }

        public void addInterfaceName(final String interfaceInternalName) {
            Assert.checkNotNullParam("interfaceInternalName", interfaceInternalName);
            String[] interfaceNames = this.interfaceNames;
            int len = interfaceNames.length;
            int interfaceCount = this.interfaceCount;
            if (interfaceCount == len) {
                // just grow it
                int newSize = len == 0 ? 4 : len << 1;
                interfaceNames = this.interfaceNames = Arrays.copyOf(interfaceNames, newSize);
            }
            interfaceNames[interfaceCount] = interfaceInternalName;
            this.interfaceCount = interfaceCount + 1;
        }

        public void setSignature(final ClassSignature signature) {
            Assert.checkNotNullParam("signature", signature);
            this.signature = signature;
        }

        public void setDescriptor(final ClassTypeDescriptor descriptor) {
            this.descriptor = Assert.checkNotNullParam("descriptor", descriptor);
        }

        public void setVisibleAnnotations(final List<Annotation> annotations) {
            this.visibleAnnotations = Assert.checkNotNullParam("annotations", annotations);
        }

        public void setInvisibleAnnotations(final List<Annotation> annotations) {
            this.invisibleAnnotations = Assert.checkNotNullParam("annotations", annotations);
        }

        public void setVisibleTypeAnnotations(final TypeAnnotationList annotationList) {
            this.visibleTypeAnnotations = Assert.checkNotNullParam("annotationList", annotationList);
        }

        public void setInvisibleTypeAnnotations(final TypeAnnotationList annotationList) {
            this.invisibleTypeAnnotations = Assert.checkNotNullParam("annotationList", annotationList);
        }

        public void setBootstrapMethods(final List<BootstrapMethod> bootstrapMethods) {
            this.bootstrapMethods = Assert.checkNotNullParam("bootstrapMethods", bootstrapMethods);
        }

        public void setSuperClass(final DefinedTypeDefinition superClass) {
            this.superClass = superClass;
        }

        public void setName(final String internalName) {
            this.internalName = Assert.checkNotNullParam("internalName", internalName);
        }

        public void setModifiers(final int modifiers) {
            this.modifiers = modifiers;
        }

        public void addModifiers(int modifiers) {
            this.modifiers |= modifiers;
        }

        public void setSuperClassName(final String superClassInternalName) {
            this.superClassName = superClassInternalName;
        }

        public DefinedTypeDefinition build() {
            return new DefinedTypeDefinitionImpl(this);
        }

        public Location getLocation() {
            return Location.builder().setClassInternalName(internalName).build();
        }
    }


    // todo: move to common utils?

    private static final VarHandle intArrayHandle = MethodHandles.arrayElementVarHandle(int[].class);
    private static final VarHandle intArrayArrayHandle = MethodHandles.arrayElementVarHandle(int[][].class);
    private static final VarHandle stringArrayHandle = MethodHandles.arrayElementVarHandle(String[].class);
    private static final VarHandle annotationArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[].class);
    private static final VarHandle annotationArrayArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[][].class);
    private static final VarHandle annotationArrayArrayArrayHandle = MethodHandles.arrayElementVarHandle(Annotation[][][].class);

    private static String getVolatile(String[] array, int index) {
        return (String) stringArrayHandle.getVolatile(array, index);
    }

    private static int getVolatile(int[] array, int index) {
        return (int) intArrayHandle.getVolatile(array, index);
    }

    private static int[] getVolatile(int[][] array, int index) {
        return (int[]) intArrayArrayHandle.getVolatile(array, index);
    }

    private static Annotation[][] getVolatile(Annotation[][][] array, int index) {
        return (Annotation[][]) annotationArrayArrayArrayHandle.getVolatile(array, index);
    }

    private static Annotation[] getVolatile(Annotation[][] array, int index) {
        return (Annotation[]) annotationArrayArrayHandle.getVolatile(array, index);
    }

    private static Annotation getVolatile(Annotation[] array, int index) {
        return (Annotation) annotationArrayHandle.getVolatile(array, index);
    }

    private static void putVolatile(Annotation[][] array, int index, Annotation[] value) {
        annotationArrayArrayHandle.setVolatile(array, index, value);
    }

    private static String setIfNull(String[] array, int index, String newVal) {
        while (! stringArrayHandle.compareAndSet(array, index, null, newVal)) {
            String appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    private static int[] setIfNull(int[][] array, int index, int[] newVal) {
        while (! intArrayArrayHandle.compareAndSet(array, index, null, newVal)) {
            int[] appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    private static Annotation[] setIfNull(Annotation[][] array, int index, Annotation[] newVal) {
        while (! annotationArrayArrayHandle.compareAndSet(array, index, null, newVal)) {
            Annotation[] appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    private static Annotation setIfNull(Annotation[] array, int index, Annotation newVal) {
        while (! annotationArrayHandle.compareAndSet(array, index, null, newVal)) {
            Annotation appearing = getVolatile(array, index);
            if (appearing != null) {
                return appearing;
            }
        }
        return newVal;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean equals(final Object obj) {
        return obj instanceof DefinedTypeDefinitionImpl ? super.equals(obj) : obj.equals(this);
    }
}
