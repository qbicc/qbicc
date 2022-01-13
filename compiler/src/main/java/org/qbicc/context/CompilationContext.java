package org.qbicc.context;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.machine.arch.Platform;
import org.qbicc.object.Function;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.NativeMethodConfigurator;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.MemberElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public interface CompilationContext extends DiagnosticContext {

    String IMPLICIT_SECTION_NAME = "__implicit__";

    String INITIAL_HEAP_SECTION_NAME = "QBICC_I_HEAP";

    Platform getPlatform();

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    ClassContext getBootstrapClassContext();

    default ClassContext getClassContextForLoader(VmClassLoader classLoaderObject) {
        return classLoaderObject == null ? getBootstrapClassContext() : classLoaderObject.getClassContext();
    }

    ClassContext constructClassContext(VmClassLoader classLoaderObject);

    /**
     * @deprecated
     */
    MethodElement getVMHelperMethod(String helperName);

    /**
     * @deprecated
     */
    MethodElement getOMHelperMethod(String helperName);

    void enqueue(ExecutableElement element);

    boolean wasEnqueued(ExecutableElement element);

    NativeMethodConfigurator getNativeMethodConfigurator();

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

    /**
     * AutoQueued elements are non-entrypoint executable elements that the
     * compiler has determined must be included in the final image.
     * Typically this is because they have been entered into a
     * dispatching table or they are a runtime function that is guaranteed to
     * be invoked by expansions in the LOWER phase.
     * These capability should be used sparingly, since AutoQueued elements
     * (and all elements and types that are indirectly reachable from them)
     * will be included in the final executable.
     *
     * @param element the element to register as an auto-queued element.
     */
    void registerAutoQueuedElement(ExecutableElement element);

    Path getOutputDirectory();

    Path getOutputFile(DefinedTypeDefinition type, String suffix);

    Path getOutputDirectory(DefinedTypeDefinition type);

    Path getOutputDirectory(MemberElement element);

    ProgramModule getProgramModule(final DefinedTypeDefinition type);

    ProgramModule getOrAddProgramModule(DefinedTypeDefinition type);

    List<ProgramModule> getAllProgramModules();

    DefinedTypeDefinition getDefaultTypeDefinition();

    Section getImplicitSection(ExecutableElement element);

    Section getImplicitSection(DefinedTypeDefinition typeDefinition);

    Function getExactFunction(ExecutableElement element);

    Function getExactFunctionIfExists(ExecutableElement element);

    FunctionElement establishExactFunction(ExecutableElement element, FunctionElement function);

    FunctionType getFunctionTypeForElement(ExecutableElement element);

    FunctionType getFunctionTypeForInitializer();

    FieldElement getExceptionField();

    Vm getVm();

    /**
     * Set the task runner used to run parallel tasks on task threads. The runner can wrap the task in various ways.
     *
     * @param taskRunner the task runner (must not be {@code null})
     * @throws IllegalStateException if this method is called from a compiler thread, or if the compiler threads are not
     *  running
     */
    void setTaskRunner(BiConsumer<Consumer<CompilationContext>, CompilationContext> taskRunner) throws IllegalStateException;

    /**
     * Run a task on every compiler thread.  When the task has returned on all threads, this method will return.  This
     * method must not be called from a compiler thread or an exception will be thrown.
     *
     * @param task the task to run on every compiler thread
     * @throws IllegalStateException if this method is called from a compiler thread, or if the compiler threads are not
     *  running
     */
    void runParallelTask(Consumer<CompilationContext> task) throws IllegalStateException;

    /**
     * Get the copier for the current phase.
     *
     * @return the copier (not {@code null})
     * @throws IllegalStateException if the current phase does not have a copier
     */
    BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> getCopier();
}
