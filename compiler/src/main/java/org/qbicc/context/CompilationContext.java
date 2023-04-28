package org.qbicc.context;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.qbicc.facts.Facts;
import org.qbicc.facts.core.ExecutableReachabilityFacts;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.schedule.Scheduler;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.machine.arch.Platform;
import org.qbicc.object.Function;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.NativeMethodConfigurator;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.MemberElement;

/**
 *
 */
public interface CompilationContext extends DiagnosticContext {

    String IMPLICIT_SECTION_NAME = "__implicit__";

    Platform getPlatform();

    TypeSystem getTypeSystem();

    Scheduler getScheduler();

    LiteralFactory getLiteralFactory();

    ClassContext getBootstrapClassContext();

    ClassContext getClassContextForLoader(VmClassLoader classLoaderObject);

    ClassContext constructClassContext(VmClassLoader classLoaderObject);

    ClassContext constructAppClassLoaderClassContext(VmClassLoader appClassLoaderObject);

    ClassContext constructPlatformClassContext(VmClassLoader platformClassLoaderObject);

    <T> void submitTask(T item, Consumer<T> itemConsumer);

    default void enqueue(ExecutableElement element) {
        Facts.get(this).discover(element, ExecutableReachabilityFacts.IS_INVOKED);
    }

    /**
     * Determine whether the given element was already enqueued in the <em>current</em> phase.  Note that unless this
     * is used in a post-hook, an element could become enqueued by another thread at any time, so it should only
     * be used as a check (optimization) to short-circuit an otherwise idempotent operation in that case.
     *
     * @param element the element to check
     * @return {@code true} if the element was already enqueued in the <em>current</em> phase, or {@code false} otherwise
     */
    default boolean wasEnqueued(ExecutableElement element) {
        return Facts.get(this).isDiscovered(element, ExecutableReachabilityFacts.IS_INVOKED);
    }

    /**
     * Determine whether the given element was enqueued in the previous phase and thus is eligible to be enqueued again in
     * <em>this</em> phase.
     *
     * @param element the element to check
     * @return {@code true} if the element may be enqueued in this phase, or {@code false} if enqueuing the element will
     * result in an exception
     */
    default boolean mayBeEnqueued(ExecutableElement element) {
        Facts facts = Facts.get(this);
        return !facts.hasPreviousFacts() || facts.hadFact(element, ExecutableReachabilityFacts.IS_INVOKED);
    }

    default int numberEnqueued() {
        return (int) Facts.get(this).getDiscoveredCount(ExecutableReachabilityFacts.IS_INVOKED);
    }

    NativeMethodConfigurator getNativeMethodConfigurator();

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

    default ProgramModule getOrAddProgramModule(MemberElement element) {
        return getOrAddProgramModule(element.getEnclosingType());
    }

    ProgramModule getOrAddProgramModule(DefinedTypeDefinition type);

    List<ProgramModule> getAllProgramModules();

    DefinedTypeDefinition getDefaultTypeDefinition();

    ModuleSection getImplicitSection(ExecutableElement element);

    ModuleSection getImplicitSection(DefinedTypeDefinition typeDefinition);

    /**
     * Get the implicit section.
     * This is a section which holds all external declarations and data.
     * The implicit section is not emitted in the final program.
     *
     * @return the implicit section
     */
    Section getImplicitSection();

    Function getExactFunction(ExecutableElement element);

    Function getExactFunctionIfExists(ExecutableElement element);

    FunctionElement establishExactFunction(ExecutableElement element, FunctionElement function);

    FunctionType getFunctionTypeForInvokableType(InvokableType origType);

    FunctionType getFunctionTypeForElement(ExecutableElement element);

    FunctionType getFunctionTypeForInitializer();

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
     * Run a task with the currently set task runner.
     *
     * @param task the task to run (must not be {@code null})
     */
    void runWrappedTask(Consumer<CompilationContext> task);

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
    BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock>, NodeVisitor<Node.Copier, Value, Node, BasicBlock>> getCopier();

}
