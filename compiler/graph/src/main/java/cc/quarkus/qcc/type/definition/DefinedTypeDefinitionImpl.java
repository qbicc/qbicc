package cc.quarkus.qcc.type.definition;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;

import cc.quarkus.qcc.interpreter.JavaClass;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.interpreter.JavaVM;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class DefinedTypeDefinitionImpl implements DefinedTypeDefinition {
    private final JavaObject definingClassLoader;
    private final String internalName;
    private final String superClassName;
    private final int modifiers;
    private final String[] interfaceNames;
    private final MethodResolver[] methodResolvers;
    private final int[] methodIndexes;
    private final FieldResolver[] fieldResolvers;
    private final int[] fieldIndexes;
    private final ConstructorResolver[] constructorResolvers;
    private final int[] constructorIndexes;
    private final InitializerResolver initializerResolver;
    private final int initializerIndex;
    private final Annotation[] visibleAnnotations;
    private final Annotation[] invisibleAnnotations;
    private final JavaClass javaClass;

    private volatile DefinedTypeDefinition verified;

    private static final String[] NO_INTERFACES = new String[0];
    private static final int[] NO_INTS = new int[0];
    private static final int[][] NO_INT_ARRAYS = new int[0][];
    private static final MethodResolver[] NO_METHODS = new MethodResolver[0];
    private static final FieldResolver[] NO_FIELDS = new FieldResolver[0];
    private static final ConstructorResolver[] NO_CONSTRUCTORS = new ConstructorResolver[0];
    private static final Annotation[][] NO_ANNOTATION_ARRAYS = new Annotation[0][];
    private static final Annotation[][][] NO_ANNOTATION_ARRAY_ARRAYS = new Annotation[0][][];

    DefinedTypeDefinitionImpl(final BuilderImpl builder) {
        this.javaClass = Assert.checkNotNullParam("builder.javaClass", builder.javaClass);
        this.definingClassLoader = builder.definingClassLoader;
        this.internalName = Assert.checkNotNullParam("builder.internalName", builder.internalName);
        this.superClassName = builder.superClassName;
        this.modifiers = builder.modifiers;
        int interfaceCount = builder.interfaceCount;
        this.interfaceNames = interfaceCount == 0 ? NO_INTERFACES : Arrays.copyOf(builder.interfaceNames, interfaceCount);
        int methodCount = builder.methodCount;
        this.methodResolvers = methodCount == 0 ? NO_METHODS : Arrays.copyOf(builder.methodResolvers, methodCount);
        this.methodIndexes = methodCount == 0 ? NO_INTS : Arrays.copyOf(builder.methodIndexes, methodCount);
        int fieldCount = builder.fieldCount;
        this.fieldResolvers = fieldCount == 0 ? NO_FIELDS : Arrays.copyOf(builder.fieldResolvers, fieldCount);
        this.fieldIndexes = fieldCount == 0 ? NO_INTS : Arrays.copyOf(builder.fieldIndexes, fieldCount);
        int constructorCount = builder.constructorCount;
        this.constructorResolvers = constructorCount == 0 ? NO_CONSTRUCTORS : Arrays.copyOf(builder.constructorResolvers, constructorCount);
        this.constructorIndexes = constructorCount == 0 ? NO_INTS : Arrays.copyOf(builder.constructorIndexes, constructorCount);
        int annotationCount = builder.visibleAnnotationCount;
        this.visibleAnnotations = annotationCount == 0 ? Annotation.NO_ANNOTATIONS : Arrays.copyOf(builder.visibleAnnotations, annotationCount);
        annotationCount = builder.invisibleAnnotationCount;
        this.invisibleAnnotations = annotationCount == 0 ? Annotation.NO_ANNOTATIONS : Arrays.copyOf(builder.invisibleAnnotations, annotationCount);
        this.initializerResolver = builder.initializerResolver;
        this.initializerIndex = builder.initializerIndex;
    }

    public JavaClass getJavaClass() {
        return javaClass;
    }

    public String getInternalName() {
        return internalName;
    }

    public boolean internalNameEquals(final String internalName) {
        return this.internalName.equals(Assert.checkNotNullParam("internalName", internalName));
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

    public VerifiedTypeDefinition verify() throws VerifyFailedException {
        DefinedTypeDefinition verified = this.verified;
        if (verified != null) {
            return verified.verify();
        }
        VerifiedTypeDefinition superType;
        if (superClassName != null) {
            superType = JavaVM.currentThread().getVM().loadClass(definingClassLoader, superClassName).getTypeDefinition();
        } else {
            superType = null;
        }
        int cnt = getInterfaceCount();
        VerifiedTypeDefinition[] interfaces = new VerifiedTypeDefinition[cnt];
        for (int i = 0; i < cnt; i ++) {
            interfaces[i] = JavaVM.currentThread().getVM().loadClass(definingClassLoader, getInterfaceInternalName(i)).getTypeDefinition();
        }
        cnt = getFieldCount();
        FieldElement[] fields = cnt == 0 ? FieldElement.NO_FIELDS : new FieldElement[cnt];
        for (int i = 0; i < cnt; i ++) {
            fields[i] = fieldResolvers[i].resolveField(fieldIndexes[i]);
        }
        cnt = getMethodCount();
        MethodElement[] methods = cnt == 0 ? MethodElement.NO_METHODS : new MethodElement[cnt];
        for (int i = 0; i < cnt; i ++) {
            methods[i] = methodResolvers[i].resolveMethod(methodIndexes[i]);
        }
        cnt = getConstructorCount();
        ConstructorElement[] ctors = cnt == 0 ? ConstructorElement.NO_CONSTRUCTORS : new ConstructorElement[cnt];
        for (int i = 0; i < cnt; i ++) {
            ctors[i] = constructorResolvers[i].resolveConstructor(constructorIndexes[i]);
        }
        InitializerElement init = initializerResolver.resolveInitializer(initializerIndex);
        synchronized (this) {
            verified = this.verified;
            if (verified != null) {
                return verified.verify();
            }
            try {
                verified = new VerifiedTypeDefinitionImpl(this, superType, interfaces, fields, methods, ctors, init);
            } catch (VerifyFailedException e) {
                this.verified = new VerificationFailedDefinitionImpl(this, e.getMessage(), e.getCause());
                throw e;
            }
            // replace in the map *first*, *then* replace our local ref
//            definingLoader.replaceTypeDefinition(name, this, verified);
            this.verified = verified;
            return verified.verify();
        }
    }

    public JavaObject getDefiningClassLoader() {
        return definingClassLoader;
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

    public int getVisibleAnnotationCount() {
        return visibleAnnotations.length;
    }

    public Annotation getVisibleAnnotation(final int index) {
        return visibleAnnotations[index];
    }

    public int getInvisibleAnnotationCount() {
        return invisibleAnnotations.length;
    }

    public Annotation getInvisibleAnnotation(final int index) {
        return invisibleAnnotations[index];
    }

    public boolean hasSuperClass() {
        return superClassName != null;
    }

    // internal

    static class BuilderImpl implements Builder {
        JavaObject definingClassLoader;
        JavaClass javaClass;
        String internalName;
        String superClassName = "java/lang/Object";
        int modifiers = ClassFile.ACC_SUPER;
        int interfaceCount;
        String[] interfaceNames;
        int methodCount;
        MethodResolver[] methodResolvers;
        int[] methodIndexes;
        int fieldCount;
        FieldResolver[] fieldResolvers;
        int[] fieldIndexes;
        int constructorCount;
        ConstructorResolver[] constructorResolvers;
        int[] constructorIndexes;
        InitializerResolver initializerResolver = InitializerResolver.EMPTY;
        int initializerIndex;
        int visibleAnnotationCount;
        Annotation[] visibleAnnotations;
        int invisibleAnnotationCount;
        Annotation[] invisibleAnnotations;

        public void setJavaClass(final JavaClass javaClass) {
            this.javaClass = Assert.checkNotNullParam("javaClass", javaClass);
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
                int newSize = ((len << 1) + len) >> 1;
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
                int newSize = ((len << 1) + len) >> 1;
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
                int newSize = ((len << 1) + len) >> 1;
                constructorResolvers = this.constructorResolvers = Arrays.copyOf(constructorResolvers, newSize);
                constructorIndexes = this.constructorIndexes = Arrays.copyOf(constructorIndexes, newSize);
            }
            constructorResolvers[constructorCount] = resolver;
            constructorIndexes[constructorCount] = index;
            this.constructorCount = constructorCount + 1;
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
                int newSize = ((len << 1) + len) >> 1;
                interfaceNames = this.interfaceNames = Arrays.copyOf(interfaceNames, newSize);
            }
            interfaceNames[interfaceCount] = interfaceInternalName;
            this.interfaceCount = interfaceCount + 1;
        }

        public void expectVisibleAnnotationCount(final int count) {
            Assert.checkMinimumParameter("count", 0, count);
            if (count == 0) {
                return;
            }
            Annotation[] annotations = this.visibleAnnotations;
            if (annotations == null) {
                this.visibleAnnotations = new Annotation[count];
            } else if (annotations.length < count) {
                this.visibleAnnotations = Arrays.copyOf(annotations, count);
            }
        }

        public void addVisibleAnnotation(final Annotation annotation) {
            Assert.checkNotNullParam("annotation", annotation);
            Annotation[] annotations = this.visibleAnnotations;
            int annotationCount = this.visibleAnnotationCount;
            if (annotations == null) {
                annotations = this.visibleAnnotations = new Annotation[4];
            } else {
                int len = annotations.length;
                if (annotationCount == len) {
                    // just grow it
                    int newSize = ((len << 1) + len) >> 1;
                    annotations = this.visibleAnnotations = Arrays.copyOf(annotations, newSize);
                }
            }
            annotations[annotationCount] = annotation;
            this.visibleAnnotationCount = annotationCount + 1;
        }

        public void expectInvisibleAnnotationCount(final int count) {
            Assert.checkMinimumParameter("count", 0, count);
            if (count == 0) {
                return;
            }
            Annotation[] annotations = this.invisibleAnnotations;
            if (annotations == null) {
                this.invisibleAnnotations = new Annotation[count];
            } else if (annotations.length < count) {
                this.invisibleAnnotations = Arrays.copyOf(annotations, count);
            }
        }

        public void addInvisibleAnnotation(final Annotation annotation) {
            Assert.checkNotNullParam("annotation", annotation);
            Annotation[] annotations = this.invisibleAnnotations;
            int annotationCount = this.invisibleAnnotationCount;
            if (annotations == null) {
                annotations = this.invisibleAnnotations = new Annotation[4];
            } else {
                int len = annotations.length;
                if (annotationCount == len) {
                    // just grow it
                    int newSize = ((len << 1) + len) >> 1;
                    annotations = this.invisibleAnnotations = Arrays.copyOf(annotations, newSize);
                }
            }
            annotations[annotationCount] = annotation;
            this.invisibleAnnotationCount = annotationCount + 1;
        }

        public void setDefiningClassLoader(JavaObject definingClassLoader) {
            this.definingClassLoader = definingClassLoader;
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

}
