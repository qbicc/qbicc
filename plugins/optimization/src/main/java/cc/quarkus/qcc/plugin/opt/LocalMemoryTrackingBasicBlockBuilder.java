package cc.quarkus.qcc.plugin.opt;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.ElementOf;
import cc.quarkus.qcc.graph.MemberOf;
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
            Value value = handle.accept(this, mode);
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

    @Override
    public Value visitUnknown(MemoryAtomicityMode param, ValueHandle node) {
        return knownValues.get(node);
    }

    @Override
    public Value visit(MemoryAtomicityMode param, ElementOf node) {
        Value value = knownValues.get(node);
        if (value != null) {
            return value;
        } else {
            value = node.getValueHandle().accept(this, param);
            if (value != null) {
                return extractElement(value, node.getIndex());
            } else {
                return null;
            }
        }
    }

    @Override
    public Value visit(MemoryAtomicityMode param, MemberOf node) {
        Value value = knownValues.get(node);
        if (value != null) {
            return value;
        } else {
            value = node.getValueHandle().accept(this, param);
            if (value != null) {
                return extractMember(value, node.getMember());
            } else {
                return null;
            }
        }
    }
}
