package cc.quarkus.qcc.context;

import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;

/**
 *
 */
public interface CompilationContext extends DiagnosticContext {

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    ClassContext getBootstrapClassContext();

    ClassContext constructClassContext(JavaObject classLoaderObject);

    void enqueue(ParameterizedExecutableElement element);

    boolean isEnqueued(ParameterizedExecutableElement element);

    ParameterizedExecutableElement dequeue();

    void registerEntryPoint(MethodElement method);
}
