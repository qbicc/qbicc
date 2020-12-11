package cc.quarkus.qcc.context;

import java.nio.file.Path;
import java.util.List;

import cc.quarkus.qcc.graph.literal.CurrentThreadLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.MemberElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

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

    void registerEntryPoint(MethodElement method);

    Path getOutputDirectory();

    Path getOutputDirectory(DefinedTypeDefinition type);

    Path getOutputDirectory(MemberElement element);

    ProgramModule getOrAddProgramModule(DefinedTypeDefinition type);

    List<ProgramModule> getAllProgramModules();

    Function getExactFunction(ExecutableElement element);

    CurrentThreadLiteral getCurrentThreadValue();
}
