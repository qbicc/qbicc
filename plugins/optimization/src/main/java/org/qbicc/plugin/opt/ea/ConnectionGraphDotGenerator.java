package org.qbicc.plugin.opt.ea;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.graph.BasicBlock;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.BasicElement;
import org.qbicc.type.definition.element.ElementVisitor;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MemberElement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

// TODO vastly copied from DotGenerator, could some code be shared?
public class ConnectionGraphDotGenerator implements ElementVisitor<CompilationContext, Void>, Consumer<CompilationContext> {

    @Override
    public void accept(CompilationContext compilationContext) {
        // TODO: register and implement for inter-method analysis
    }

    public Void visitUnknown(final CompilationContext ctxt, final BasicElement basicElement) {
        if (basicElement instanceof ExecutableElement) {
            ExecutableElement element = (ExecutableElement) basicElement;
            if (element.hasMethodBody()) {
                MethodBody methodBody = element.getMethodBody();
                process(element, methodBody);
            }
        }
        return null;
    }

    private void process(final ExecutableElement element, final MethodBody methodBody) {
        if (element.hasAllModifiersOf(ClassFile.ACC_ABSTRACT)) return;
        DefinedTypeDefinition def = element.getEnclosingType();
        CompilationContext ctxt = def.getContext().getCompilationContext();
        BasicBlock entryBlock = methodBody.getEntryBlock();
        Path dir = ctxt.getOutputDirectory(element);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            failedToWrite(ctxt, dir, e);
            return;
        }
        Path path = dir.resolve("ea-intra.dot");
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("digraph {");
            bw.newLine();
            bw.write("graph [ rankdir = LR ];");
            bw.newLine();
            bw.write("edge [ splines = true ];");
            bw.newLine();
            bw.write("node [colorscheme=pastel24];");
            bw.newLine();
            bw.write("\"Global Escape\" [style=filled fillcolor = 2];");
            bw.newLine();
            bw.write("\"Arg Escape\" [style=filled fillcolor = 3];");
            bw.newLine();
            bw.write("\"No Escape\" [style=filled fillcolor = 1];");
            bw.newLine();
            bw.write("\"Unknown\" [style=filled fillcolor = 4];");
            bw.newLine();
            bw.newLine();
            final ConnectionGraph connectionGraph = EscapeAnalysisState.get(ctxt).getConnectionGraph(element);
            ConnectionGraphDotVisitor visitor = new ConnectionGraphDotVisitor(entryBlock, connectionGraph);
            visitor.process(bw);
            bw.write("}");
        } catch (IOException e) {
            failedToWrite(ctxt, path, e);
        } catch (UncheckedIOException e) {
            IOException cause = e.getCause();
            failedToWrite(ctxt, path, cause);
        } catch (TooBigException e) {
            ctxt.warning("Element \"%s\" is too big to graph", element);
        }
    }

    private static Diagnostic failedToWrite(final CompilationContext ctxt, final Path path, final IOException cause) {
        return ctxt.warning("Failed to write \"%s\": %s", path, cause);
    }
}
