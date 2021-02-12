package cc.quarkus.qcc.plugin.conversion;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.type.IntegerType;

public class LLVMCompatibleBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public LLVMCompatibleBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value negate(Value v) {
        if (v.getType() instanceof IntegerType) {
            final IntegerLiteral zero = ctxt.getLiteralFactory().literalOf(0);
            return super.sub(zero, v);
        }
        
        return super.negate(v);
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.VOLATILE) {
            Value loaded = super.load(handle, MemoryAtomicityMode.ACQUIRE);
            fence(MemoryAtomicityMode.ACQUIRE);
            return loaded;
        } else {
            return super.load(handle, mode);
        }
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        if (mode == MemoryAtomicityMode.VOLATILE) {
            Node store = super.store(handle, value, MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT);
            fence(MemoryAtomicityMode.RELEASE);
            return store;
        } else {
            return super.store(handle, value, mode);
        }
    }
}
