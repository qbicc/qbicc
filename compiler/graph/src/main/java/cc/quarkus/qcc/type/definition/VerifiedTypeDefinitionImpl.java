package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.InterfaceType;
import cc.quarkus.qcc.graph.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 *
 */
final class VerifiedTypeDefinitionImpl implements VerifiedTypeDefinition {
    private final DefinedTypeDefinitionImpl delegate;
    private final ClassType classType;
    private final VerifiedTypeDefinition superType;
    private final VerifiedTypeDefinition[] interfaces;
    private volatile ResolvedTypeDefinition resolved;

    @Deprecated(forRemoval = true) // as soon as we don't need it anymore
    private final MethodNode[] methodNodes;

    VerifiedTypeDefinitionImpl(final DefinedTypeDefinitionImpl delegate, final VerifiedTypeDefinition superType, final VerifiedTypeDefinition[] interfaces) {
        this.delegate = delegate;
        this.superType = superType;
        this.interfaces = interfaces;
        int interfaceCnt = interfaces.length;
        InterfaceType[] interfaceTypes = new InterfaceType[interfaceCnt];
        for (int i = 0; i < interfaceCnt; i ++) {
            ClassType classType = interfaces[i].getClassType();
            if (! (classType instanceof InterfaceType)) {
                throw new VerifyFailedException("Type " + classType.getClassName() + " is not an interface");
            }
            interfaceTypes[i] = (InterfaceType) classType;
        }
        if (isInterface()) {
            classType = Type.interfaceType(this, interfaceTypes);
        } else {
            classType = Type.classType(this, superType == null ? null : superType.getClassType(), interfaceTypes);
        }
        // -----------------------------------
        // ↓↓↓ delete once we can drop asm ↓↓↓
        int methodCount = getMethodCount();
        MethodNode[] nodes = new MethodNode[methodCount];
        ClassNode classNode = new ClassNode(Dictionary.ASM_VERSION) {
            int idx = 0; // hopefully we visit in the same order
            public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                nodes[idx++] = (MethodNode) mv;
                return mv;
            }
        };
        try {
            new ClassReader(new ByteBufferInputStream(delegate.getClassBytes())).accept(classNode, 0);
        } catch (Exception e) {
            throw new VerifyFailedException(e);
        }
        methodNodes = nodes;
        // ↑↑↑ delete once we can drop asm ↑↑↑
        // -----------------------------------
    }

    // delegates

    public Dictionary getDefiningClassLoader() {
        return delegate.getDefiningClassLoader();
    }

    public String getName() {
        return delegate.getName();
    }

    public int getModifiers() {
        return delegate.getModifiers();
    }

    public String getSuperClassName() {
        return delegate.getSuperClassName();
    }

    public int getInterfaceCount() {
        return delegate.getInterfaceCount();
    }

    public String getInterfaceName(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterfaceName(index);
    }

    public int getFieldCount() {
        return delegate.getFieldCount();
    }

    public int getMethodCount() {
        return delegate.getMethodCount();
    }

    public DefinedFieldDefinition getFieldDefinition(final int index) throws IndexOutOfBoundsException {
        return delegate.getFieldDefinition(index);
    }

    public DefinedMethodDefinition getMethodDefinition(final int index) throws IndexOutOfBoundsException {
        return delegate.getMethodDefinition(index);
    }

    // local methods

    public ClassType getClassType() {
        return classType;
    }

    public VerifiedTypeDefinition getSuperClass() {
        return superType;
    }

    public VerifiedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return interfaces[index];
    }

    // next stage

    public ResolvedTypeDefinition resolve() throws ResolutionFailedException {
        ResolvedTypeDefinition resolved = this.resolved;
        if (resolved != null) {
            return resolved;
        }
        VerifiedTypeDefinition superClass = getSuperClass();
        if (superClass != null) {
            superClass.resolve();
        }
        int cnt = getInterfaceCount();
        for (int i = 0; i < cnt; i ++) {
            getInterface(i).resolve();
        }
        cnt = getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            getFieldDefinition(i).resolve();
        }
        cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            getMethodDefinition(i).resolve();
        }
        synchronized (this) {
            resolved = this.resolved;
            if (resolved != null) {
                return resolved;
            }
            resolved = new ResolvedTypeDefinitionImpl(this);
            getDefiningClassLoader().replaceTypeDefinition(getName(), this, resolved);
            this.resolved = resolved;
        }
        return resolved;
    }

    @Deprecated(forRemoval = true) // as soon as it's not needed anymore!
    MethodNode getMethodNode(final int index) {
        return methodNodes[index];
    }
}

