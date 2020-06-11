package cc.quarkus.qcc.type.definition;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.ArrayType;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptorParser;
import cc.quarkus.qcc.type.universe.Core;
import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class TypeDefinitionNode extends ClassNode implements TypeDefinition {

    volatile ClassType cachedType;

    public TypeDefinitionNode(Universe universe) {
        super(Universe.ASM_VERSION);
        this.universe = universe;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        // eagerly resolver super
        if (superName != null) {
            this.universe.findClass(superName, false);
        }
        // eagerly resolver interfaces
        if (interfaces != null) {
            for (String each : interfaces) {
                this.universe.findClass(each, false);
            }
        }
    }

    public boolean isInterface() {
        return (Opcodes.ACC_INTERFACE & this.access) != 0;
    }

    @Override
    public MethodDefinitionNode<?> visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodDescriptorParser parser = new MethodDescriptorParser(getUniverse(), this, name, descriptor, (access & Opcodes.ACC_STATIC) != 0);
        MethodDescriptor methodDescriptor = parser.parseMethodDescriptor();
        MethodDefinitionNode<?> visitor = new MethodDefinitionNode<>(this, access, name, methodDescriptor, signature, exceptions);
        this.methods.add(visitor);
        return visitor;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldDefinitionNode visitor = new FieldDefinitionNode(this, access, name, descriptor, signature, value);
        this.fields.add(visitor);
        return visitor;
    }

    @Override
    public int getAccess() {
        return this.access;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public TypeDefinition getSuperclass() {
        if (this.superName == null) {
            return null;
        }
        return this.universe.findClass(this.superName);
    }

    @Override
    public List<TypeDefinition> getInterfaces() {
        return this.interfaces.stream()
                .map(this.universe::findClass)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAssignableFrom(TypeDefinition other) {
        if (other == null) {
            return false;
        }

        if (getName().equals(other.getName())) {
            return true;
        }

        if (isAssignableFrom(other.getSuperclass())) {
            return true;
        }

        for (TypeDefinition each : other.getInterfaces()) {
            if (isAssignableFrom(each)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<MethodDefinition> getMethods() {
        return this.methods.stream()
                .map(e -> (MethodDefinition) e)
                .collect(Collectors.toList());
    }

    @Override
    public MethodDefinition findMethod(String name, String desc) {
        for (MethodNode each : this.methods) {
            if ( each.name.equals(name) && each.desc.equals(desc)) {
                return (MethodDefinition) each;
            }
        }
        if (getSuperclass() == null) {
            throw new RuntimeException("Unresolved method " + name + desc);
        }
        return getSuperclass().findMethod(name, desc);
    }

    @Override
    public MethodDefinition findMethod(MethodDescriptor methodDescriptor) {
        return findMethod(methodDescriptor.getName(), methodDescriptor.getDescriptor());
    }

    @Override
    public List<FieldDefinition> getFields() {
        return this.fields.stream().map(e -> (FieldDefinition) e).collect(Collectors.toList());
    }

    @Override
    public FieldDefinition resolveField(FieldDescriptor fieldDescriptor) {
        // JVMS 5.4.3.2. Field Resolution

        // 1. If C declares a field with the name and descriptor specified by the field reference,
        // field lookup succeeds. The declared field is the result of the field lookup.

        for (FieldDefinition field : getFields()) {
            if ( field.getName().equals(fieldDescriptor.getName()) && field.getType() == fieldDescriptor.getType() ) {
                return field;
            }
        }

        // 2. Otherwise, field lookup is applied recursively to the direct superinterfaces
        // of the specified class or interface C.

        for (TypeDefinition each : getInterfaces()) {
            FieldDefinition candidate = each.resolveField(fieldDescriptor);
            if ( candidate != null ) {
                return candidate;
            }
        }

        // 3. Otherwise, if C has a superclass S, field lookup is applied recursively to S.

        if ( getSuperclass() != null ) {
            FieldDefinition candidate = getSuperclass().resolveField(fieldDescriptor);
            if ( candidate != null ) {
                return candidate;
            }
        }

        return null;
    }

    @Override
    public MethodDefinition resolveMethod(MethodDescriptor methodDescriptor) {
        // JVMS 5.4.4.3

        // 1. If C is an interface, method resolution throws an IncompatibleClassChangeError.
        if (isInterface()) {
            throw new IncompatibleClassChangeError(this.name + " is an interface");
        }

        // 2. Otherwise, method resolution attempts to locate the referenced method in C and its superclasses:

        // 2.a If C declares exactly one method with the name specified by the method
        // reference, and the declaration is a signature polymorphic method (§2.9.3),
        // then method lookup succeeds. All the class names mentioned in the descriptor
        // are resolved (§5.4.3.1).
        //
        // The resolved method is the signature polymorphic method declaration. It is
        // not necessary for C to declare a method with the descriptor specified by the method reference.
        if ( isMethodHandleOrVarHandle() ) {
            List<MethodDefinition> candidates = getMethods().stream().filter(e -> e.getName().equals(methodDescriptor.getName())).collect(Collectors.toList());
            if ( candidates.size() == 1) {
                MethodDefinition candidate = candidates.get(0);
                if ( candidate.isVarargs() && candidate.isNative() ) {
                    if ( candidate.getParamTypes().size() == 1 ) {
                        Type theType = candidate.getParamTypes().get(0);
                        if (theType instanceof ArrayType) {
                            Type elementType = ((ArrayType) theType).getElementType();
                            if (elementType instanceof ClassType) {
                                if (((ClassType) elementType).getClassName().equals("java/lang/Object")) {
                                    return candidate;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2.b Otherwise, if C declares a method with the name and descriptor specified by
        // the method reference, method lookup succeeds.
        Optional<MethodDefinition> candidate = getMethods().stream()
                .filter(methodDescriptor::matches)
                .findFirst();

        if (candidate.isPresent()) {
            return candidate.get();
        }

        // 2.c Otherwise, if C has a superclass, step 2 of method resolution is recursively
        // invoked on the direct superclass of C.

        if ( getSuperclass() != null ) {
            MethodDefinition superCandidate = getSuperclass().resolveMethod(methodDescriptor);
            if ( superCandidate != null ) {
                return superCandidate;
            }
        }

        for (TypeDefinition each : getInterfaces()) {
            MethodDefinition interfaceCandidate = each.resolveInterfaceMethod(methodDescriptor);
        }

        return null;
    }

    public MethodDefinition resolveInterfaceMethod(MethodDescriptor methodDescriptor) {
        return resolveInterfaceMethod(methodDescriptor, false);
    }

    public MethodDefinition resolveInterfaceMethod(MethodDescriptor methodDescriptor, boolean searchingSuper) {
        // 5.4.3.4. Interface Method Resolution

        // 1. If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
        if (!isInterface()) {
            throw new IncompatibleClassChangeError(getName() + " is not an interface");
        }

        // 2. Otherwise, if C declares a method with the name and descriptor specified
        // by the interface method reference, method lookup succeeds.

        Optional<MethodDefinition> candidate = getMethods().stream()
                .filter(methodDescriptor::matches)
                .findFirst();
        if (candidate.isPresent() ) {
            MethodDefinition method = candidate.get();

            if (!searchingSuper) {
                return method;
            }

            if (!method.isPrivate() && !method.isStatic()) {
                return method;
            }
        }

        // 3. Otherwise, if the class Object declares a method with the name and descriptor
        // specified by the interface method reference, which has its ACC_PUBLIC flag set
        // and does not have its ACC_STATIC flag set, method lookup succeeds.

        MethodDefinition objectCandidate = Core.java.lang.Object().resolveMethod(methodDescriptor);
        if ( objectCandidate != null ) {
            if ( objectCandidate.isPublic() && ! objectCandidate.isStatic()) {
                return objectCandidate;
            }
        }

        // 4. Otherwise, if the maximally-specific superinterface methods (§5.4.3.3) of C
        // for the name and descriptor specified by the method reference include exactly
        // one method that does not have its ACC_ABSTRACT flag set, then this method is
        // chosen and method lookup succeeds.

        for (TypeDefinition each : getInterfaces()) {
            MethodDefinition superCandidate = getSuperclass().resolveInterfaceMethod(methodDescriptor);
            if ( superCandidate != null ) {
                return superCandidate;
            }
        }

        // 5. Otherwise, if any superinterface of C declares a method with the name and descriptor
        // specified by the method reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC
        // flag set, one of these is arbitrarily chosen and method lookup succeeds.

        for (TypeDefinition each : getInterfaces()) {
            MethodDefinition interfaceCandidate = each.resolveInterfaceMethod(methodDescriptor, true);
            if ( interfaceCandidate != null ) {
                return interfaceCandidate;
            }
        }

        return null;
    }

    protected boolean isMethodHandleOrVarHandle() {
        return this.name.equals("java/lang/invoke/MethodHandle") || this.name.equals("java/lang/invoke/VarHandle");
    }

    @Override
    public FieldDefinition findField(String name) {
        for (FieldNode field : this.fields) {
            if ( field.name.equals(name)) {
                return (FieldDefinition) field;
            }
        }

        throw new RuntimeException("Unresolved field " + name);
    }

    @Override
    public Object getStatic(FieldDefinition field) {
        return ((FieldDefinitionNode)field).value;
    }

    @Override
    public Object getField(FieldDefinition field, ObjectReference objRef) {
        return objRef.getFieldValue(field);
    }

    @Override
    public void putField(FieldDefinition field, ObjectReference objRef, Object val) {
        objRef.setFieldValue(field, val);
    }

    @Override
    public ClassType getType() {
        ClassType cachedType = this.cachedType;
        if (cachedType == null) {
            synchronized (this) {
                cachedType = this.cachedType;
                if (cachedType == null) {
                    this.cachedType = cachedType = Type.classNamed(name);
                }
            }
        }
        return cachedType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TypeDefinition) {
            return ((TypeDefinition) obj).getName().equals(this.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public Universe getUniverse() {
        return this.universe;
    }

    private final Universe universe;
}
