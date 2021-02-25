package cc.quarkus.qcc.plugin.opt;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.ValueHandleVisitor;

/**
 *
 */
public class LocalMemoryTrackingBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<MemoryAtomicityMode, Value> {
    private final CompilationContext ctxt;
    private final Map<ValueHandle, Value> knownValues = new HashMap<>();

    public LocalMemoryTrackingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        // other incoming edges might have loads which invalidate our local cache
        knownValues.clear();
        return super.begin(blockLabel);
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        // todo: hopefully we can be slightly more aggressive than this
        if (mode != MemoryAtomicityMode.NONE && mode != MemoryAtomicityMode.UNORDERED) {
            knownValues.clear();
        } else {
            // todo: use this visitor to recursively scan larger handles until we have a match
            //return handle.accept(this, mode);
            Value value = knownValues.get(handle);
            if (value != null) {
                return value;
            }
        }
        Value loaded = super.load(handle, mode);
        knownValues.put(handle, loaded);
        return loaded;
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        knownValues.put(handle, value);
        return super.store(handle, value, mode);
    }
}
