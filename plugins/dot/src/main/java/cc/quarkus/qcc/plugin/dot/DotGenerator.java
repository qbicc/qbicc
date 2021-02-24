package cc.quarkus.qcc.plugin.dot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.driver.GraphGenConfig;
import cc.quarkus.qcc.driver.GraphGenFilter;
import cc.quarkus.qcc.driver.Phase;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.object.ProgramObject;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.element.BasicElement;
import cc.quarkus.qcc.type.definition.element.Element;
import cc.quarkus.qcc.type.definition.element.ElementVisitor;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.MemberElement;

/**
 *
 */
public class DotGenerator implements ElementVisitor<CompilationContext, Void>, Consumer<CompilationContext> {
    private final Phase phase;
    private final GraphGenFilter filter;

    public DotGenerator(Phase p, GraphGenConfig graphGenConfig) {
        this.phase = p;
        if (graphGenConfig != null) {
            filter = graphGenConfig.getFilter();
        } else {
            filter = null;
        }
    }

    public void accept(final CompilationContext compilationContext) {
        for (ProgramModule module : compilationContext.getAllProgramModules()) {
            for (Section section : module.sections()) {
                for (ProgramObject content : section.contents()) {
                    if (content instanceof Function) {
                        Element element = ((Function) content).getOriginalElement();
                        if (element instanceof MemberElement) {
                            MethodBody body = ((Function) content).getBody();
                            if (body != null && filter != null && filter.accept(element, phase)) {
                                process((MemberElement) element, body);
                            }
                        }
                    }
                }
            }
        }
    }

    public Void visitUnknown(final CompilationContext param, final BasicElement basicElement) {
        if (basicElement instanceof ExecutableElement) {
            ExecutableElement element = (ExecutableElement) basicElement;
            MethodBody methodBody = element.getOrCreateMethodBody();
            if (methodBody != null && filter != null && filter.accept(element, phase)) {
                process(element, methodBody);
            }
        }
        return null;
    }

    private void process(final MemberElement element, MethodBody methodBody) {
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
        Path path = dir.resolve(phase.toString() + ".dot");
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("digraph {");
            bw.newLine();
            bw.write("graph [ rankdir = BT ];");
            bw.newLine();
            bw.write("edge [ splines = true ];");
            bw.newLine();
            bw.newLine();
            DotNodeVisitor visitor = new DotNodeVisitor(entryBlock);
            visitor.process(bw);
            bw.write("}");
        } catch (IOException e) {
            failedToWrite(ctxt, path, e);
        } catch (UncheckedIOException e) {
            IOException cause = e.getCause();
            failedToWrite(ctxt, path, cause);
        }
    }

    private static Diagnostic failedToWrite(final CompilationContext ctxt, final Path path, final IOException cause) {
        return ctxt.warning("Failed to write \"%s\": %s", path, cause);
    }
}
