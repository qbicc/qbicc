package org.qbicc.plugin.gc.common;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.Descriptor;

/**
 * Expand a multiNewArray into a loop nest that allocates and
 * connects all of the constituent one dimensional arrays.
 */
public class MultiNewArrayExpansionBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Slot TEMP0 = Slot.temp(0);

    public MultiNewArrayExpansionBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
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
        Descriptor elementDesc = desc.getElementTypeDescriptor();
        if (!(elementDesc instanceof ArrayTypeDescriptor)) {
            getContext().error(getLocation(), "Unexpected array descriptor: %s", elementDesc);
            throw new BlockEarlyTermination(unreachable());
        }
        // create a loop to create and fill each nested array
        LiteralFactory lf = getLiteralFactory();
        TypeSystem ts = getTypeSystem();
        SignedIntegerType s32 = ts.getSignedInteger32Type();
        BlockLabel loop = new BlockLabel();
        BlockLabel exit = new BlockLabel();
        BlockLabel resume = new BlockLabel();
        goto_(loop, TEMP0, lf.literalOf(s32, 0));
        begin(loop);
        BlockParameter bp = addParam(loop, TEMP0, s32);
        if_(isEq(bp, dimension), exit, resume, Map.of());
        try {
            begin(resume);
            Value innerArray = multiNewArray((ArrayTypeDescriptor) elementDesc, dimensions);
            store(elementOf(decodeReference(newArray), bp), innerArray);
            goto_(loop, TEMP0, add(bp, lf.literalOf(s32, 1)));
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        begin(exit);
        return newArray;
    }

}
