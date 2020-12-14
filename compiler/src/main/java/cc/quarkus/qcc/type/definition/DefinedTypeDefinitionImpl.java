package cc.quarkus.qcc.type.definition;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.classfile.BootstrapMethod;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.NestedClassElement;
import cc.quarkus.qcc.type.generic.ClassSignature;
import io.smallrye.common.constraint.Assert;

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
    private final ClassSignature signature;
    private final MethodResolver[] methodResolvers;
    private final int[] methodIndexes;
    private final FieldResolver[] fieldResolvers;
    private final int[] fieldIndexes;
    private final ConstructorResolver[] constructorResolvers;
    private final int[] constructorIndexes;
    private final InitializerResolver initializerResolver;
    private final int initializerIndex;
    private final List<Annotation> visibleAnnotations;
    private final List<Annotation> invisibleAnnotations;
    private final TypeAnnotationList visibleTypeAnnotations;
    private final TypeAnnotationList invisibleTypeAnnotations;
    private final List<BootstrapMethod> bootstrapMethods;
    private final EnclosingClassResolver enclosingClassResolver;
    private final int enclosingClassResolverIndex;
    private final EnclosedClassResolver[] enclosedClassResolvers;
    private final int[] enclosedClassResolverIndexes;

    private volatile DefinedTypeDefinition validated;

    private static final String[] NO_INTERFACES = new String[0];
    private static final int[] NO_INTS = new int[0];
    private static final MethodResolver[] NO_METHODS = new MethodResolver[0];
    private static final FieldResolver[] NO_FIELDS = new FieldResolver[0];
    private static final ConstructorResolver[] NO_CONSTRUCTORS = new ConstructorResolver[0];
    private static final EnclosedClassResolver[] NO_ENCLOSED = new EnclosedClassResolver[0];

    DefinedTypeDefinitionImpl(final BuilderImpl builder) {
        this.context = builder.context;
        this.internalName = Assert.checkNotNullParam("builder.internalName", builder.internalName);
        this.superClassName = builder.superClassName;
        String simpleName = builder.simpleName;
        if (simpleName == null) {
            int idx = internalName.lastIndexOf('/');
            this.simpleName = idx == -1 ? internalName : internalName.substring(idx + 1);
        } else {
            this.simpleName = simpleName;
        }
        this.modifiers = builder.modifiers;
        int interfaceCount = builder.interfaceCount;
        this.interfaceNames = interfaceCount == 0 ? NO_INTERFACES : Arrays.copyOf(builder.interfaceNames, interfaceCount);
        int methodCount = builder.methodCount;
        this.signature = Assert.checkNotNullParam("builder.signature", builder.signature);
        this.methodResolvers = methodCount == 0 ? NO_METHODS : Arrays.copyOf(builder.methodResolvers, methodCount);
        this.methodIndexes = methodCount == 0 ? NO_INTS : Arrays.copyOf(builder.methodIndexes, methodCount);
        int fieldCount = builder.fieldCount;
        this.fieldResolvers = fieldCount == 0 ? NO_FIELDS : Arrays.copyOf(builder.fieldResolvers, fieldCount);
        this.fieldIndexes = fieldCount == 0 ? NO_INTS : Arrays.copyOf(builder.fieldIndexes, fieldCount);
        int constructorCount = builder.constructorCount;
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
        int enclosedClassCount = builder.enclosedClassCount;
        this.enclosedClassResolvers = enclosedClassCount == 0 ? NO_ENCLOSED : Arrays.copyOf(builder.enclosedClassResolvers, enclosedClassCount);
        this.enclosedClassResolverIndexes = enclosedClassCount == 0 ? NO_INTS : Arrays.copyOf(builder.enclosedClassResolverIndexes, enclosedClassCount);
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

    public ClassSignature getSignature() {
        return signature;
    }

    public int getModifiers() {
        return modifiers;
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

    public ValidatedTypeDefinition validate() throws VerifyFailedException {
        DefinedTypeDefinition validated = this.validated;
        if (validated != null) {
            return validated.validate();
        }
        ValidatedTypeDefinition superType;
        if (superClassName != null) {
            DefinedTypeDefinition definedSuperType = context.findDefinedType(superClassName);
            if (definedSuperType == null) {
                throw new VerifyFailedException("Failed to load super class " + superClassName);
            }
            superType = definedSuperType.validate();
        } else {
            superType = null;
        }
        int cnt = getInterfaceCount();
        ValidatedTypeDefinition[] interfaces = new ValidatedTypeDefinition[cnt];
        for (int i = 0; i < cnt; i ++) {
            interfaces[i] = context.findDefinedType(getInterfaceInternalName(i)).validate();
        }
        cnt = getFieldCount();
        FieldElement[] fields = cnt == 0 ? FieldElement.NO_FIELDS : new FieldElement[cnt];
        for (int i = 0; i < cnt; i ++) {
            fields[i] = fieldResolvers[i].resolveField(fieldIndexes[i], this);
        }
        cnt = getMethodCount();
        MethodElement[] methods = cnt == 0 ? MethodElement.NO_METHODS : new MethodElement[cnt];
        for (int i = 0; i < cnt; i ++) {
            methods[i] = methodResolvers[i].resolveMethod(methodIndexes[i], this);
        }
        cnt = getConstructorCount();
        ConstructorElement[] ctors = cnt == 0 ? ConstructorElement.NO_CONSTRUCTORS : new ConstructorElement[cnt];
        for (int i = 0; i < cnt; i ++) {
            ctors[i] = constructorResolvers[i].resolveConstructor(constructorIndexes[i], this);
        }
        InitializerElement init = initializerResolver.resolveInitializer(initializerIndex, this);
        NestedClassElement enclosingClass = enclosingClassResolver == null ? null : enclosingClassResolver.resolveEnclosingNestedClass(enclosingClassResolverIndex, this);
        NestedClassElement[] enclosedClasses = resolveEnclosedClasses(enclosedClassResolvers, enclosedClassResolverIndexes, 0, 0);
        synchronized (this) {
            validated = this.validated;
            if (validated != null) {
                return validated.validate();
            }
            try {
                validated = new ValidatedTypeDefinitionImpl(this, superType, interfaces, fields, methods, ctors, init, enclosingClass, enclosedClasses);
            } catch (VerifyFailedException e) {
                this.validated = new VerificationFailedDefinitionImpl(this, e.getMessage(), e.getCause());
                throw e;
            }
            // replace in the map *first*, *then* replace our local ref
//            definingLoader.replaceTypeDefinition(name, this, verified);
            this.validated = validated;
            return validated.validate();
        }
    }

    private NestedClassElement[] resolveEnclosedClasses(final EnclosedClassResolver[] resolvers, final int[] indexes, final int inIdx, final int outIdx) {
        int maxInIdx = resolvers.length;
        if (inIdx == maxInIdx) {
            return outIdx == 0 ? NestedClassElement.NO_NESTED_CLASSES : new NestedClassElement[outIdx];
        }
        NestedClassElement resolved = resolvers[inIdx].resolveEnclosedNestedClass(indexes[inIdx], this);
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
        String[] interfaceNames = NO_INTERFACES;
        ClassSignature signature;
        int methodCount;
        MethodResolver[] methodResolvers = NO_METHODS;
        int[] methodIndexes = NO_INTS;
        int fieldCount;
        FieldResolver[] fieldResolvers = NO_FIELDS;
        int[] fieldIndexes = NO_INTS;
        int constructorCount;
        ConstructorResolver[] constructorResolvers = NO_CONSTRUCTORS;
        int[] constructorIndexes = NO_INTS;
        InitializerResolver initializerResolver;
        int initializerIndex;
        List<Annotation> visibleAnnotations = List.of();
        List<Annotation> invisibleAnnotations = List.of();
        TypeAnnotationList visibleTypeAnnotations = TypeAnnotationList.empty();
        TypeAnnotationList invisibleTypeAnnotations = TypeAnnotationList.empty();
        List<BootstrapMethod> bootstrapMethods = List.of();
        String simpleName;
        EnclosingClassResolver enclosingClassResolver;
        int enclosingClassResolverIndex;
        int enclosedClassCount;
        EnclosedClassResolver[] enclosedClassResolvers = NO_ENCLOSED;
        int[] enclosedClassResolverIndexes = NO_INTS;

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
            } else if (fieldResolvers.length < count) {
                this.fieldResolvers = Arrays.copyOf(fieldResolvers, count);
                this.fieldIndexes = Arrays.copyOf(fieldIndexes, count);
            }
        }

        public void addField(final FieldResolver resolver, final int index) {
            Assert.checkNotNullParam("resolver", resolver);
            FieldResolver[] fieldResolvers = this.fieldResolvers;
            int[] fieldIndexes = this.fieldIndexes;
            int len = fieldResolvers.length;
            int fieldCount = this.fieldCount;
            if (fieldCount == len) {
                // just grow it
                int newSize = len == 0 ? 4 : len << 1;
                fieldResolvers = this.fieldResolvers = Arrays.copyOf(fieldResolvers, newSize);
                fieldIndexes = this.fieldIndexes = Arrays.copyOf(fieldIndexes, newSize);
            }
            fieldResolvers[fieldCount] = resolver;
            fieldIndexes[fieldCount] = index;
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
            } else if (methodResolvers.length < count) {
                this.methodResolvers = Arrays.copyOf(methodResolvers, count);
                this.methodIndexes = Arrays.copyOf(methodIndexes, count);
            }
        }

        public void addMethod(final MethodResolver resolver, final int index) {
            Assert.checkNotNullParam("resolver", resolver);
            MethodResolver[] methodResolvers = this.methodResolvers;
            int[] methodIndexes = this.methodIndexes;
            int len = methodResolvers.length;
            int methodCount = this.methodCount;
            if (methodCount == len) {
                // just grow it
                int newSize = len == 0 ? 4 : len << 1;
                methodResolvers = this.methodResolvers = Arrays.copyOf(methodResolvers, newSize);
                methodIndexes = this.methodIndexes = Arrays.copyOf(methodIndexes, newSize);
            }
            methodResolvers[methodCount] = resolver;
            methodIndexes[methodCount] = index;
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
            } else if (constructorResolvers.length < count) {
                this.constructorResolvers = Arrays.copyOf(constructorResolvers, count);
                this.constructorIndexes = Arrays.copyOf(constructorIndexes, count);
            }
        }

        public void addConstructor(final ConstructorResolver resolver, final int index) {
            Assert.checkNotNullParam("resolver", resolver);
            ConstructorResolver[] constructorResolvers = this.constructorResolvers;
            int[] constructorIndexes = this.constructorIndexes;
            int len = constructorResolvers.length;
            int constructorCount = this.constructorCount;
            if (constructorCount == len) {
                // just grow it
                int newSize = len == 0 ? 4 : len << 1;
                constructorResolvers = this.constructorResolvers = Arrays.copyOf(constructorResolvers, newSize);
                constructorIndexes = this.constructorIndexes = Arrays.copyOf(constructorIndexes, newSize);
            }
            constructorResolvers[constructorCount] = resolver;
            constructorIndexes[constructorCount] = index;
            this.constructorCount = constructorCount + 1;
        }

        public void setSimpleName(final String simpleName) {
            Assert.checkNotNullParam("simpleName", simpleName);
            this.simpleName = simpleName;
        }

        public void setEnclosingClass(final EnclosingClassResolver resolver, final int index) {
            Assert.checkNotNullParam("resolver", resolver);
            this.enclosingClassResolver = resolver;
            this.enclosingClassResolverIndex = index;
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

        public void setName(final String internalName) {
            this.internalName = Assert.checkNotNullParam("internalName", internalName);
        }

        public void setModifiers(final int modifiers) {
            this.modifiers = modifiers;
        }

        public void setSuperClassName(final String superClassInternalName) {
            this.superClassName = superClassInternalName;
        }

        public DefinedTypeDefinition build() {
            return new DefinedTypeDefinitionImpl(this);
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
