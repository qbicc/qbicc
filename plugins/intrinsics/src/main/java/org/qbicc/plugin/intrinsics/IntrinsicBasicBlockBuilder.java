package org.qbicc.plugin.intrinsics;

import java.util.List;
import java.util.Map;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.jboss.logging.Logger;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.InstanceMethodElementHandle;
import org.qbicc.graph.Slot;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;

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
    public Value call(PointerValue target, List<Value> arguments) {
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle decodedTarget = (StaticMethodElementHandle) target;
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    decodedTarget,
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        } else if (target instanceof InstanceMethodElementHandle) {
            InstanceMethodElementHandle decodedTarget = (InstanceMethodElementHandle) target;
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value instance = decodedTarget.getInstance();
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    instance,
                    decodedTarget,
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        }
        return super.call(target, arguments);
    }

    @Override
    public Value callNoSideEffects(PointerValue target, List<Value> arguments) {
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle decodedTarget = (StaticMethodElementHandle) target;
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    decodedTarget,
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        } else if (target instanceof InstanceMethodElementHandle) {
            InstanceMethodElementHandle decodedTarget = (InstanceMethodElementHandle) target;
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value instance = decodedTarget.getInstance();
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    instance,
                    decodedTarget,
                    arguments);
                if (result != null) {
                    return result;
                }
            }
        }
        return super.callNoSideEffects(target, arguments);
    }

    @Override
    public BasicBlock callNoReturn(PointerValue target, List<Value> arguments) {
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle decodedTarget = (StaticMethodElementHandle) target;
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                intrinsic.emitIntrinsic(getFirstBuilder(),
                    decodedTarget,
                    arguments);
                return unreachable();
            }
        } else if (target instanceof InstanceMethodElementHandle) {
            InstanceMethodElementHandle decodedTarget = (InstanceMethodElementHandle) target;
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value instance = decodedTarget.getInstance();
                intrinsic.emitIntrinsic(getFirstBuilder(),
                    instance,
                    decodedTarget,
                    arguments);
                return unreachable();
            }
        }
        return super.callNoReturn(target, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(PointerValue target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle decodedTarget = (StaticMethodElementHandle) target;
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                intrinsic.emitIntrinsic(getFirstBuilder(),
                    decodedTarget,
                    arguments);
                return unreachable();
            }
        } else if (target instanceof InstanceMethodElementHandle) {
            InstanceMethodElementHandle decodedTarget = (InstanceMethodElementHandle) target;
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value instance = decodedTarget.getInstance();
                intrinsic.emitIntrinsic(getFirstBuilder(),
                    instance,
                    decodedTarget,
                    arguments);
                return unreachable();
            }
        }
        return super.invokeNoReturn(target, arguments, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(PointerValue target, List<Value> arguments) {
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle decodedTarget = (StaticMethodElementHandle) target;
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    decodedTarget,
                    arguments);
                if (result != null) {
                    return return_(result);
                }
            }
        } else if (target instanceof InstanceMethodElementHandle) {
            InstanceMethodElementHandle decodedTarget = (InstanceMethodElementHandle) target;
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value instance = decodedTarget.getInstance();
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    instance,
                    decodedTarget,
                    arguments);
                if (result != null) {
                    return return_(result);
                }
            }
        }
        return super.tailCall(target, arguments);
    }

    @Override
    public BasicBlock tailInvoke(PointerValue target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle decodedTarget = (StaticMethodElementHandle) target;
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    decodedTarget,
                    arguments);
                if (result != null) {
                    return return_(result);
                }
            }
        } else if (target instanceof InstanceMethodElementHandle) {
            InstanceMethodElementHandle decodedTarget = (InstanceMethodElementHandle) target;
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value instance = decodedTarget.getInstance();
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    instance,
                    decodedTarget,
                    arguments);
                if (result != null) {
                    return return_(result);
                }
            }
        }
        return super.tailInvoke(target, arguments, catchLabel, targetArguments);
    }

    @Override
    public Value invoke(PointerValue target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle decodedTarget = (StaticMethodElementHandle) target;
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    (StaticMethodElementHandle) target,
                    arguments);
                if (result != null) {
                    ImmutableMap<Slot, Value> immutableMap = Maps.immutable.ofMap(targetArguments);
                    goto_(resumeLabel, immutableMap.newWithKeyValue(Slot.result(), result).castToMap());
                    return result;
                }
            }
        } else if (target instanceof InstanceMethodElementHandle) {
            InstanceMethodElementHandle decodedTarget = (InstanceMethodElementHandle) target;
            // instance
            InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value instance = decodedTarget.getInstance();
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    instance,
                    (InstanceMethodElementHandle) target,
                    arguments);
                if (result != null) {
                    ImmutableMap<Slot, Value> immutableMap = Maps.immutable.ofMap(targetArguments);
                    goto_(resumeLabel, immutableMap.newWithKeyValue(Slot.result(), result).castToMap());
                    return result;
                }
            }
        }
        return super.invoke(target, arguments, catchLabel, resumeLabel, targetArguments);
    }

}
