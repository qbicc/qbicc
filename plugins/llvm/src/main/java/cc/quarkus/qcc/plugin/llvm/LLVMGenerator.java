package cc.quarkus.qcc.plugin.llvm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.object.Data;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.object.ProgramObject;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;

/**
 *
 */
public class LLVMGenerator implements Consumer<CompilationContext> {
    public void accept(final CompilationContext ctxt) {
        for (ProgramModule programModule : ctxt.getAllProgramModules()) {
            DefinedTypeDefinition def = programModule.getTypeDefinition();
            Path path = ctxt.getOutputDirectory(def);
            final Module module = Module.newModule();
            for (Section section : programModule.sections()) {
                String sectionName = section.getName();
                for (ProgramObject item : section.contents()) {
                    String name = item.getName();
                    if (item instanceof Function) {
                        MethodBody body = ((Function) item).getBody();
                        if (body == null) {
                            ctxt.error("Function `%s` has no body", name);
                            continue;
                        }
                        BasicBlock entryBlock = body.getEntryBlock();
                        FunctionDefinition functionDefinition = module.define(name);
                        LLVMNodeVisitor nodeVisitor = new LLVMNodeVisitor(ctxt, Schedule.forMethod(entryBlock), ((Function) item), functionDefinition);
                        if (! sectionName.equals(CompilationContext.IMPLICIT_SECTION_NAME)) {
                            functionDefinition.section(sectionName);
                        }

                        nodeVisitor.execute();
                    } else {
                        assert item instanceof Data;
                        // todo: support data output
                    }
                }
            }
            Path outputFile = path.resolve("output.ll");
            try {
                Files.createDirectories(outputFile.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                    module.writeTo(writer);
                }
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
    }
}
