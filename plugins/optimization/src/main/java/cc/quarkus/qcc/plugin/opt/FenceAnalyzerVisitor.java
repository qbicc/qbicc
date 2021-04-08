package cc.quarkus.qcc.plugin.opt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Load;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Node.Copier;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.Store;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;

/**
 * A visitor which collects relations among volatile accesses.
 */
public class FenceAnalyzerVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
    private static final AttachmentKey<FenceAnalyzerVisitor> KEY = new AttachmentKey<>();

    private final CompilationContext context;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;
    private UDChain<Load, Store> ud;
    private Map<Node, Node> newValues = new HashMap<Node, Node>();

    public FenceAnalyzerVisitor(final CompilationContext context, final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate) {
        this.context = context;
        this.delegate = delegate;
        ud = new UDChain<Load, Store>();
    }

    public static FenceAnalyzerVisitor getAnalyzer(CompilationContext ctxt) {
        return ctxt.getAttachment(KEY);
    }

    public UDChain<Load, Store> getUDChain() {
        return ud;
    }

    public Map<Node, Node> getNewValueMap() {
        return newValues;
    }

    public NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }

    // Add store regardless of the value.
    private BiFunction<Load, Node, Boolean> addVolatileStore = (u, d) -> {
        if (d instanceof Store
                && (((Store) d).getMode() == MemoryAtomicityMode.VOLATILE)) {
            ud.addDef((Store) d, u);
            return true;
        }
        return false;
    };

    public Node visit(final Node.Copier param, Store node) {
        Node stored = getDelegateActionVisitor().visit(param, node);

        if (node.getMode() == MemoryAtomicityMode.VOLATILE) {
            newValues.put(node, stored);
            context.putAttachment(KEY, this);
        }

        return stored;
    }

    /**
     * Construct UD chain by following dependency chains.
     */
    public Value visit(final Node.Copier param, Load node) {
        Load loaded = (Load) getDelegateValueVisitor().visit(param, node);

        if (node.getMode() == MemoryAtomicityMode.VOLATILE) {
            ud.constructUD(node, addVolatileStore);
            newValues.put(node, loaded);
            context.putAttachment(KEY, this);
        }

        return loaded;
    }
}
