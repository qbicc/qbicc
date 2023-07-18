package org.qbicc.plugin.llvm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.driver.Driver;
import org.qbicc.machine.tool.CCompilerInvoker;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.tool.llvm.LlcInvoker;
import org.qbicc.tool.llvm.LlvmToolChain;
import org.qbicc.tool.llvm.OutputFormat;
import org.qbicc.tool.llvm.RelocationModel;
import org.qbicc.type.definition.LoadedTypeDefinition;

public class LLVMCompilerImpl implements LLVMCompiler {
    private final boolean useCcForIr;
    private final boolean emitIr;
    private final boolean emitAssembly;
    private final LlcInvoker llcInvoker;
    private final CCompilerInvoker ccInvoker;
    private final boolean compileOutput;

    public LLVMCompilerImpl(final CompilationContext ctxt, final LLVMConfiguration config, final LLVMModuleGenerator generator) {
        useCcForIr = config.isWasm();
        emitIr = config.isEmitIr();
        emitAssembly = config.isEmitAssembly() && ! useCcForIr;
        if (useCcForIr) {
            llcInvoker = null;
        } else {
            llcInvoker = createLlcInvoker(ctxt, config);
            if (llcInvoker != null) {
                if (emitAssembly) {
                    llcInvoker.setOutputFormat(OutputFormat.ASM);
                } else {
                    llcInvoker.setOutputFormat(OutputFormat.OBJ);
                }
            }
        }
        ccInvoker = createCCompilerInvoker(ctxt);
        if (ccInvoker != null) {
            if (useCcForIr) {
                ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.LLVM_IR);
            } else {
                ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.ASM);
            }
        }
        this.compileOutput = config.isCompileOutput();
    }

    @Override
    public void compileModule(final CompilationContext ctxt, LoadedTypeDefinition typeDefinition, LLVMModuleGenerator moduleGenerator) {
        final Path directory = ctxt.getOutputDirectory(typeDefinition);
        final Path objectFile = ctxt.getOutputFile(typeDefinition, ctxt.getPlatform().getObjectType().objectSuffix());
        final Path irFile = ctxt.getOutputFile(typeDefinition, "ll");
        final Path asmFile = ctxt.getOutputFile(typeDefinition, "s");
        final ProgramModule programModule = ctxt.getOrAddProgramModule(typeDefinition);
        final InputSource generatorSource = InputSource.from(writer -> {
            try (final BufferedWriter bw = new BufferedWriter(writer)) {
                moduleGenerator.processProgramModule(programModule, bw, irFile);
            }
        }, StandardCharsets.UTF_8);
        if (! compileOutput) {
            // don't produce output, just run the generator
            try {
                generatorSource.transferTo(OutputDestination.discarding());
            } catch (IOException ignored) {
                // unlikely; it would have been reported already in any event
            }
            return;
        }
        try {
            Files.createDirectories(directory.getParent());
        } catch (IOException e) {
            ctxt.error(Location.builder().setType(typeDefinition).build(), "Failed to create directory %s: %s", directory, e.toString());
            return;
        }
        if (emitIr) {
            try {
                generatorSource.transferTo(OutputDestination.of(irFile));
            } catch (IOException e) {
                ctxt.error(Location.builder().setSourceFilePath(irFile.toString()).build(), "Error writing LLVM IR file: %s", e.toString());
                return;
            }
            if (useCcForIr) {
                ccInvoker.setSource(InputSource.from(irFile));
            } else {
                llcInvoker.setSource(InputSource.from(irFile));
            }
        } else {
            // compile directly from the source
            if (useCcForIr) {
                ccInvoker.setSource(generatorSource);
            } else {
                llcInvoker.setSource(generatorSource);
            }
        }
        if (useCcForIr) {
            ccInvoker.setOutputPath(objectFile);
        } else {
            llcInvoker.setDestination(OutputDestination.of(emitAssembly ? asmFile : objectFile));
        }
        // invoke LLC
        int errCnt = ctxt.errors();
        try {
            if (useCcForIr) {
                ccInvoker.invoke();
            } else {
                llcInvoker.invoke();
            }
        } catch (IOException e) {
            if (errCnt == ctxt.errors()) {
                // whatever the problem was, it wasn't reported, so add the additional error here
                ctxt.error(Location.builder().setSourceFilePath(irFile.toString()).build(), "`llc` invocation has failed: %s", e.toString());
            }
            return;
        }
        if (emitAssembly && ! useCcForIr) {
            // now compile the assembly
            ccInvoker.setSource(InputSource.from(asmFile));
            ccInvoker.setOutputPath(objectFile);
            try {
                ccInvoker.invoke();
            } catch (IOException e) {
                ctxt.error("Compiler invocation has failed for %s: %s", asmFile, e.toString());
                return;
            }
        }
        Linker.get(ctxt).addObjectFilePath(typeDefinition, objectFile);
    }

    private static CCompilerInvoker createCCompilerInvoker(CompilationContext context) {
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);
        if (cToolChain == null) {
            context.error("No C tool chain is available");
            return null;
        }
        CCompilerInvoker ccInvoker = cToolChain.newCompilerInvoker();
        ccInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        return ccInvoker;
    }

    private static LlcInvoker createLlcInvoker(CompilationContext context, LLVMConfiguration config) {
        LlvmToolChain llvmToolChain = context.getAttachment(Driver.LLVM_TOOL_KEY);
        if (llvmToolChain == null) {
            context.error("No LLVM tool chain is available");
            return null;
        }
        LlcInvoker llcInvoker = llvmToolChain.newLlcInvoker();
        llcInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        llcInvoker.setRelocationModel(config.isPie() ? RelocationModel.Pic : RelocationModel.Static);
        llcInvoker.setOptions(config.getLlcOptions());
        llcInvoker.setOutputFormat(OutputFormat.ASM);
        return llcInvoker;
    }
}
