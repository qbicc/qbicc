package cc.quarkus.qcc.compiler.native_image.llvm.generic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ElementVisitor;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

final class LLVMElementVisitor implements ElementVisitor<CompilationContext, Void> {

    LLVMElementVisitor() {
    }

    public Void visit(final CompilationContext ctxt, final ConstructorElement element) {
        // todo: visit FunctionElement instead
        final Module module = Module.newModule();
        MethodBody methodBody = element.getMethodBody().getOrCreateMethodBody();
        BasicBlock entryBlock = methodBody.getEntryBlock();
        String funcName = "ctor"; // todo: get from function
        LLVMNodeVisitor nodeVisitor = new LLVMNodeVisitor(ctxt, Schedule.forMethod(entryBlock), element, module.define(funcName));
        nodeVisitor.execute();
        write(ctxt, element, module);
        return null;
    }

    public Void visit(final CompilationContext ctxt, final MethodElement element) {
        // todo: visit FunctionElement instead
        final Module module = Module.newModule();
        MethodBody methodBody = element.getMethodBody().getOrCreateMethodBody();
        BasicBlock entryBlock = methodBody.getEntryBlock();
        String funcName = element.getName(); // todo: get from function
        LLVMNodeVisitor nodeVisitor = new LLVMNodeVisitor(ctxt, Schedule.forMethod(entryBlock), element, module.define(funcName));
        nodeVisitor.execute();
        write(ctxt, element, module);
        return null;
    }

    private void write(final CompilationContext ctxt, final ExecutableElement element, final Module module) {
        Path outputDirectory = ctxt.getOutputDirectory(element);
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            ctxt.error("Failed to create directory \"%s\": %s", outputDirectory, e.getMessage());
            return;
        }
        // todo: might be better to have different granularity of ll files than method granularity
        Path outputFile = outputDirectory.resolve("output.ll");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            module.writeTo(writer);
        } catch (IOException e) {
            ctxt.error("Failed to write \"%s\": %s", outputFile, e.getMessage());
            try {
                Files.deleteIfExists(outputFile);
            } catch (IOException e2) {
                ctxt.warning("Failed to clean \"%s\": %s", outputFile, e.getMessage());
            }
        }
        LLVMState llvmState = ctxt.computeAttachmentIfAbsent(LLVMState.KEY, LLVMState::new);
        llvmState.addModulePath(outputFile);
    }

    public Void visit(final CompilationContext ctxt, final InitializerElement element) {
        ctxt.error(element, "Cannot lower class initializer");
        return null;
    }
}
