package org.qbicc.plugin.dot;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Node;
import org.qbicc.type.definition.element.ExecutableElement;

import java.util.Map;

public class DotGenerationContext {
    public final CompilationContext ctxt;
    public final ExecutableElement element;
    public final Map<Node, String> visited;

    public DotGenerationContext(CompilationContext ctxt, ExecutableElement element, Map<Node, String> visited) {
        this.ctxt = ctxt;
        this.element = element;
        this.visited = visited;
    }
}
