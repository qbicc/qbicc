package cc.quarkus.qcc.type;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class TypeDefinitionNode extends ClassNode implements TypeDefinition {

    public TypeDefinitionNode(Universe universe) {
        super(Universe.ASM_VERSION);
        this.universe = universe;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        // eagerly resolver super
        if ( superName != null ) {
            this.universe.findClass(superName, true);
        }
        // eagerly resolver interfaces
        if (interfaces != null ) {
            for (String each : interfaces) {
                this.universe.findClass(each, true);
            }
        }
    }

    @Override
    public MethodDefinitionNode visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodDefinitionNode visitor = new MethodDefinitionNode(this, access, name, descriptor, signature, exceptions);
        this.methods.add(visitor);
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
                .map(e-> this.universe.findClass(e))
                .collect(Collectors.toList());
    }

    @Override
    public Set<MethodDefinition> getMethods() {
        return this.methods.stream()
                .map(e -> (MethodDefinition) e)
                .collect(Collectors.toSet());
    }

    @Override
    public MethodDefinition getMethod(String name, String desc) {
        System.err.println("my methods: " + this.methods);
        for (MethodNode each : this.methods) {
            System.err.println("compare: " + name + "/" + desc + " vs " + each.name + "/" + each.desc);
            if ( each.name.equals(name) && each.desc.equals(desc)) {
                return (MethodDefinition) each;
            }
        }
        throw new RuntimeException("Unresolved method " + name + desc);
        //return null;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof TypeDefinition ) {
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
