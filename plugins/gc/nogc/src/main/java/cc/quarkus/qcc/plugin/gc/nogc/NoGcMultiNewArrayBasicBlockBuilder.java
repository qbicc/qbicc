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

/**
 *
 */
public class NoGcMultiNewArrayBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NoGcMultiNewArrayBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        return multiNewArray(arrayType, dimensions.iterator());
    }

    private Value multiNewArray(final ArrayObjectType arrayType, final Iterator<Value> dimensions) {
        Value dimension = dimensions.next();
        Value newArray = newArray(arrayType, dimension);
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
        ValueType elementType = arrayType.getElementType();
        ArrayObjectType nestedType;
        if (! (elementType instanceof ReferenceType)) {
            ctxt.error(getLocation(), "Unexpected array element type: %s", elementType);
            throw new BlockEarlyTermination(unreachable());
        }
        ObjectType upperBound = ((ReferenceType) elementType).getUpperBound();
        if (! (upperBound instanceof ArrayObjectType)) {
            ctxt.error(getLocation(), "Unexpected array element upper bound: %s", upperBound);
            throw new BlockEarlyTermination(unreachable());
        }
        nestedType = (ArrayObjectType) upperBound;
        Value innerArray = multiNewArray(nestedType, dimensions);
        store(elementOf(referenceHandle(innerArray), phi), innerArray, MemoryAtomicityMode.UNORDERED);
        BasicBlock loopExit = goto_(loop);
        phi.setValueForBlock(ctxt, getCurrentElement(), loopExit, sub(phi, lf.literalOf(1)));
        begin(new BlockLabel());
        return newArray;
    }

}
