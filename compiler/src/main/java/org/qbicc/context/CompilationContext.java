package org.qbicc.context;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.interpreter.VmObject;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MemberElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public interface CompilationContext extends DiagnosticContext {

    String IMPLICIT_SECTION_NAME = "__implicit__";

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    ClassContext getBootstrapClassContext();

    ClassContext constructClassContext(VmObject classLoaderObject);

    MethodElement getVMHelperMethod(String helperName);

    void enqueue(ExecutableElement element);

    boolean wasEnqueued(ExecutableElement element);

    ExecutableElement dequeue();

    /**
     * EntryPoints form the "root set" of methods that must be included
     * in the final image.  The initial set of EntryPoints must be added
     * prior to the start of the {@link org.qbicc.driver.Phase#ADD}.
     * After that, only `method`s that have been previously processed
     * during the ADD Phase can be included in the root set.
     *
     * @param method The methodElement to register as an entrypoint
     */
    void registerEntryPoint(ExecutableElement method);

    Path getOutputDirectory();

    Path getOutputFile(DefinedTypeDefinition type, String suffix);

    Path getOutputDirectory(DefinedTypeDefinition type);

    Path getOutputDirectory(MemberElement element);

    ProgramModule getOrAddProgramModule(DefinedTypeDefinition type);

    List<ProgramModule> getAllProgramModules();

    Section getImplicitSection(ExecutableElement element);

    Section getImplicitSection(DefinedTypeDefinition typeDefinition);

    Function getExactFunction(ExecutableElement element);

    FunctionType getFunctionTypeForElement(ExecutableElement element);

    FunctionDeclaration declareForeignFunction(ExecutableElement target, Function function, ExecutableElement current);

    SymbolLiteral getCurrentThreadLocalSymbolLiteral();

    FieldElement getExceptionField();

    /**
     * Run a task on every compiler thread.  When the task has returned on all threads, this method will return.  This
     * method must not be called from a compiler thread or an exception will be thrown.
     *
     * @param task the task to run on every compiler thread
     * @throws IllegalStateException if this method is called from a compiler thread, or if the compiler threads are not
     *  running
     */
    void runParallelTask(Consumer<CompilationContext> task) throws IllegalStateException;
}
