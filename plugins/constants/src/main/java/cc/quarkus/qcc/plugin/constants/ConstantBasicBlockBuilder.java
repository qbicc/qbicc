package cc.quarkus.qcc.plugin.constants;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.StaticField;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;

/**
 * A basic block builder which substitutes reads from constant static fields with the constant value of the field.
 */
public class ConstantBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ConstantBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        if (handle instanceof StaticField) {
            Value constantValue = Constants.get(ctxt).getConstantValue(((StaticField) handle).getVariableElement());
            if (constantValue != null) {
                return constantValue;
            }
        }
        return getDelegate().load(handle, mode);
    }
}
