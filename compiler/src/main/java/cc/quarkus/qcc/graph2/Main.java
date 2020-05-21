package cc.quarkus.qcc.graph2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cc.quarkus.qcc.type.universe.Universe;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
public class Main {
    public static void main(String[] args) throws IOException {
        List<GraphBuilder> gbs = new ArrayList<>();
        try (InputStream is = Test.class.getResourceAsStream("Test.class")) {
            ClassReader cr = new ClassReader(is);
            cr.accept(new ClassVisitor(Universe.ASM_VERSION) {
                public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
                    int pc = Type.getArgumentTypes(descriptor).length;
                    GraphBuilder gb = new GraphBuilder(pc, access);
                    gbs.add(gb);
                    return gb;
                }
            }, 0);
        }
        for (GraphBuilder gb : gbs) {
            System.out.println("---");
            System.out.println("digraph gr {");
            System.out.println("  graph [fontname = \"helvetica\",fontsize=10,ordering=in,outputorder=depthfirst,ranksep=1];");
            System.out.println("  node [fontname = \"helvetica\",fontsize=10,ordering=in];");
            System.out.println("  edge [fontname = \"helvetica\",fontsize=10];");
            gb.firstBlock.writeToGraph(new HashSet<>(), System.out);
            System.out.println("}");
            System.out.println("---");
        }
        // now print out the program. why not

    }
}
