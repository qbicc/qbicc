package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.graph.BooleanType;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.FloatType;
import cc.quarkus.qcc.graph.IntegerType;
import cc.quarkus.qcc.graph.PointerType;
import cc.quarkus.qcc.graph.ReferenceType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.graph.WordType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static cc.quarkus.qcc.interpreter.CodegenUtils.arrayOf;
import static cc.quarkus.qcc.interpreter.CodegenUtils.ci;
import static cc.quarkus.qcc.interpreter.CodegenUtils.p;

public class PrototypeGenerator {
    private static Map<DefinedTypeDefinition, Prototype> prototypes = new HashMap<>();

    public static Prototype getPrototype(DefinedTypeDefinition definition) {
        return prototypes.computeIfAbsent(definition, PrototypeGenerator::generate);
    }

    private static Prototype generate(DefinedTypeDefinition defined) {
        VerifiedTypeDefinition verified = defined.verify();
        ClassType classType = verified.getClassType();
        String className = classType.getClassName();

        ClassType superType = classType.getSuperClass();
        String superName;

        if (superType == null) {
            superName = "java/lang/Object";
        } else {
            superName = superType.getClassName();

            Prototype superProto = generate(superType.getDefinition());
        }

        ClassWriter proto = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        proto.visit(Opcodes.V9, verified.getModifiers(), p(className), null, p(superName), arrayOf(p(JavaObject.class)));

        verified.eachField(
                (field) -> proto.visitField(
                        field.getModifiers(),
                        field.getName(),
                        ci(javaTypeFromFieldType(field.resolve().getType())),
                        null, null));

        proto.visitEnd();

        byte[] bytecode = proto.toByteArray();

        return () -> bytecode;
    }

    private static Class javaTypeFromFieldType(Type type) {
        Class fieldType;

        if (type instanceof ClassType) {
            // reference type
            fieldType = Object.class;
        } else if (type instanceof WordType) {
            // primitive type
            fieldType = wordTypeToClass((WordType) type);
        } else {
            throw new IllegalStateException("unhandled type: " + type);
        }

        return fieldType;
    }

    private static Class wordTypeToClass(WordType wordType) {
        if (wordType instanceof BooleanType) {
            return boolean.class;
        } else if (wordType instanceof IntegerType) {
            switch (wordType.getSize()) {
                case 1:
                    return byte.class;
                case 2:
                    return short.class;
                case 4:
                    return int.class;
                case 8:
                    return long.class;
            }
        } else if (wordType instanceof FloatType) {
            switch (wordType.getSize()) {
                case 4:
                    return float.class;
                case 8:
                    return double.class;
            }
        }
        throw new IllegalArgumentException("unknown word type: " + wordType);
    }
}
