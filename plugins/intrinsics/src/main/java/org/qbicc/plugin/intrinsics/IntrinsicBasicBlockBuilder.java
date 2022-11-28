package org.qbicc.plugin.intrinsics;

import java.util.List;
import java.util.Map;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.jboss.logging.Logger;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.AbstractMethodLookup;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.InstanceMethodLiteral;
import org.qbicc.graph.literal.StaticMethodLiteral;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 * The basic block builder which substitutes invocations of intrinsic methods.
 * 
 * This only handles the "unresolved" form of method calls and assumes that all
 * methods to be replaced by intrinsics originate from descriptors (ie: classfile
 * parsing).
 */
public final class IntrinsicBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public static final Logger log = Logger.getLogger("org.qbicc.plugin.intrinsics");

    private final CompilationContext ctxt;
    private final Phase phase;

    private IntrinsicBasicBlockBuilder(final BasicBlockBuilder delegate, Phase phase) {
        super(delegate);
        this.phase = phase;
        this.ctxt = getContext();
    }

    public static IntrinsicBasicBlockBuilder createForAddPhase(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(delegate, Phase.ADD);
    }

    public static IntrinsicBasicBlockBuilder createForAnalyzePhase(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(delegate, Phase.ANALYZE);
    }

    public static IntrinsicBasicBlockBuilder createForLowerPhase(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(delegate, Phase.LOWER);
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        if (targetPtr instanceof StaticMethodLiteral lit) {
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    lit,
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        } else if (targetPtr instanceof InstanceMethodLiteral lit) {
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    receiver,
                    lit,
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        } else if (targetPtr instanceof AbstractMethodLookup lookup) {
            // instance (virtual)
            InstanceMethodElement method = lookup.getMethod();
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, method);
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    receiver,
                    getLiteralFactory().literalOf(method),
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        }
        return super.call(targetPtr, receiver, arguments);
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        if (targetPtr instanceof StaticMethodLiteral lit) {
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    lit,
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        } else if (targetPtr instanceof InstanceMethodLiteral lit) {
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    receiver,
                    lit,
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        }
        return super.callNoSideEffects(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        if (targetPtr instanceof StaticMethodLiteral lit) {
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                intrinsic.emitIntrinsic(getFirstBuilder(),
                    lit,
                    arguments);
                return unreachable();
            }
        } else if (targetPtr instanceof InstanceMethodLiteral lit) {
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                intrinsic.emitIntrinsic(getFirstBuilder(),
                    receiver,
                    lit,
                    arguments);
                return unreachable();
            }
        }
        return super.callNoReturn(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        if (targetPtr instanceof StaticMethodLiteral lit) {
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                intrinsic.emitIntrinsic(getFirstBuilder(),
                    lit,
                    arguments);
                return unreachable();
            }
        } else if (targetPtr instanceof InstanceMethodLiteral lit) {
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                intrinsic.emitIntrinsic(getFirstBuilder(),
                    receiver,
                    lit,
                    arguments);
                return unreachable();
            }
        }
        return super.invokeNoReturn(targetPtr, receiver, arguments, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        if (targetPtr instanceof StaticMethodLiteral lit) {
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    lit,
                    arguments);
                if (result != null) {
                    return return_(result);
                }
            }
        } else if (targetPtr instanceof InstanceMethodLiteral lit) {
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    receiver,
                    lit,
                    arguments);
                if (result != null) {
                    return return_(result);
                }
            }
        }
        return super.tailCall(targetPtr, receiver, arguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        if (targetPtr instanceof StaticMethodLiteral lit) {
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    lit,
                    arguments);
                if (result != null) {
                    ImmutableMap<Slot, Value> immutableMap = Maps.immutable.ofMap(targetArguments);
                    goto_(resumeLabel, immutableMap.newWithKeyValue(Slot.result(), result).castToMap());
                    return result;
                }
            }
        } else if (targetPtr instanceof InstanceMethodLiteral lit) {
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, lit.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    receiver,
                    lit,
                    arguments);
                if (result != null) {
                    ImmutableMap<Slot, Value> immutableMap = Maps.immutable.ofMap(targetArguments);
                    goto_(resumeLabel, immutableMap.newWithKeyValue(Slot.result(), result).castToMap());
                    return result;
                }
            }
        }
        return super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

}
