package cc.quarkus.qcc.plugin.gc.nogc;

import java.util.Iterator;
import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockEarlyTermination;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.Descriptor;

/**
 *
 */
public class NoGcMultiNewArrayBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NoGcMultiNewArrayBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        return multiNewArray(desc, dimensions.iterator());
    }

    private Value multiNewArray(final ArrayTypeDescriptor desc, final Iterator<Value> dimensions) {
        Value dimension = dimensions.next();
        Value newArray = newArray(desc, dimension);
        if (! dimensions.hasNext()) {
            return newArray;
        }
        // create a loop to create and fill each nested array
        BlockLabel loop = new BlockLabel();
        BasicBlock initial = goto_(loop);
        begin(loop);
        PhiValue phi = phi(dimension.getType(), loop);
        BlockLabel exit = new BlockLabel();
        BlockLabel resume = new BlockLabel();
        LiteralFactory lf = ctxt.getLiteralFactory();
        if_(isEq(phi, dimension), exit, resume);
        begin(resume);
        phi.setValueForBlock(ctxt, getCurrentElement(), initial, lf.literalOf(0));
        Descriptor elementDesc = desc.getElementTypeDescriptor();
        if (! (elementDesc instanceof ArrayTypeDescriptor)) {
            ctxt.error(getLocation(), "Unexpected array descriptor: %s", elementDesc);
            throw new BlockEarlyTermination(unreachable());
        }
        Value innerArray = multiNewArray((ArrayTypeDescriptor) elementDesc, dimensions);
        store(elementOf(referenceHandle(newArray), phi), innerArray, MemoryAtomicityMode.UNORDERED);
        BasicBlock loopExit = goto_(loop);
        phi.setValueForBlock(ctxt, getCurrentElement(), loopExit, add(phi, lf.literalOf(1)));
        begin(exit);
        return newArray;
    }

}
