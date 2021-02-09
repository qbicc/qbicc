package cc.quarkus.qcc.context;

import java.nio.file.Path;
import java.util.List;

import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.FunctionDeclaration;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MemberElement;

/**
 *
 */
public interface CompilationContext extends DiagnosticContext {

    String IMPLICIT_SECTION_NAME = "__implicit__";

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    ClassContext getBootstrapClassContext();

    ClassContext constructClassContext(VmObject classLoaderObject);

    void enqueue(ExecutableElement element);

    boolean wasEnqueued(ExecutableElement element);

    ExecutableElement dequeue();

    /**
     * EntryPoints form the "root set" of methods that must be included
     * in the final image.  The initial set of EntryPoints must be added
     * prior to the start of the {@link cc.quarkus.qcc.driver.Phase#ADD}.
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

    Function getExactFunction(ExecutableElement element);

    FunctionDeclaration declareForeignFunction(ExecutableElement target, Function function, ExecutableElement current);

    SymbolLiteral getCurrentThreadLocalSymbolLiteral();

    FieldElement getExceptionField();
}
