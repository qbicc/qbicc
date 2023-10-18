package org.qbicc.plugin.correctness;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;

/**
 * A basic block builder which materializes hidden constraints based on control flow.
 */
public final class ConstraintMaterializingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public ConstraintMaterializingBasicBlockBuilder(FactoryContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public BasicBlock if_(Value condition, BlockLabel trueTarget, BlockLabel falseTarget, Map<Slot, Value> targetArguments) {
        boolean trueIsConstrained = false;
        boolean falseIsConstrained = false;
        if (! targetArguments.isEmpty()) {
            for (Map.Entry<Slot, Value> entry : targetArguments.entrySet()) {
                Value value = entry.getValue();
                if (! trueIsConstrained && ! condition.getValueIfTrue(this, value).equals(value)) {
                    trueIsConstrained = true;
                    if (falseIsConstrained) {
                        break;
                    }
                }
                if (! falseIsConstrained && ! condition.getValueIfFalse(this, value).equals(value)) {
                    falseIsConstrained = true;
                    if (trueIsConstrained) {
                        break;
                    }
                }
            }
        }
        BlockLabel originalTrueTarget = null;
        if (trueIsConstrained) {
            originalTrueTarget = trueTarget;
            trueTarget = new BlockLabel();
        }
        BlockLabel originalFalseTarget = null;
        if (falseIsConstrained) {
            originalFalseTarget = falseTarget;
            falseTarget = new BlockLabel();
        }
        BasicBlock if_ = super.if_(condition, trueTarget, falseTarget, trueIsConstrained && falseIsConstrained ? Map.of() : targetArguments);
        if (trueIsConstrained) {
            BlockLabel finalOriginalTrueTarget = originalTrueTarget;
            begin(trueTarget, nb -> {
                nb.goto_(finalOriginalTrueTarget, transform(targetArguments, Function.identity(), input -> condition.getValueIfTrue(this, input)));
            });
        }
        if (falseIsConstrained) {
            BlockLabel finalOriginalFalseTarget = originalFalseTarget;
            begin(falseTarget, nb -> {
                nb.goto_(finalOriginalFalseTarget, transform(targetArguments, Function.identity(), input -> condition.getValueIfFalse(this, input)));
            });
        }
        return if_;
    }

    private static <K1, K2, V1, V2> Map<K2, V2> transform(Map<K1, V1> map, Function<K1, K2> keyTransform, Function<V1, V2> valueTransform) {
        return map.entrySet().stream()
            .map(entry -> Map.entry(keyTransform.apply(entry.getKey()), valueTransform.apply(entry.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
