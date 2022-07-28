package org.qbicc.plugin.lowering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.type.ObjectType;

/**
 * A basic block builder which lowers type-based {@code switch} to integer-based {@code switch}.
 */
public class TypeSwitchLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    /**
     * Construct a new instance.
     *
     * @param ctxt the compilation context (must not be {@code null})
     * @param delegate the delegate block builder (must not be {@code null})
     */
    public TypeSwitchLoweringBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public BasicBlock typeSwitch(Value value, Map<ObjectType, BlockLabel> valueToTargetMap, BlockLabel defaultTarget) {
        ArrayList<ObjectType> typesList = new ArrayList<>(valueToTargetMap.keySet());
        // ensure ordering
        typesList.sort(Comparator.comparingInt(TypeSwitchLoweringBasicBlockBuilder::getTypeId));
        // create mapping arrays
        int[] checkValues = typesList.stream().mapToInt(TypeSwitchLoweringBasicBlockBuilder::getTypeId).toArray();
        BlockLabel[] targets = typesList.stream().map(valueToTargetMap::get).toArray(BlockLabel[]::new);
        return switch_(value, checkValues, targets, defaultTarget);
    }

    private static int getTypeId(ObjectType t) {
        return t.getDefinition().load().getTypeId();
    }
}
