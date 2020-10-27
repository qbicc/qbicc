package cc.quarkus.qcc.context;

import java.nio.file.Path;

import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.BasicElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public interface CompilationContext extends DiagnosticContext {

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    ClassContext getBootstrapClassContext();

    ClassContext constructClassContext(JavaObject classLoaderObject);

    void enqueue(ExecutableElement element);

    boolean wasEnqueued(ExecutableElement element);

    ExecutableElement dequeue();

    void registerEntryPoint(MethodElement method);

    Path getOutputDirectory();

    Path getOutputDirectory(DefinedTypeDefinition type);

    Path getOutputDirectory(BasicElement element);
}
