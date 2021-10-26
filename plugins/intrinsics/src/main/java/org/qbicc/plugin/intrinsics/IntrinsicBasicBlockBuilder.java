package org.qbicc.plugin.intrinsics;

import java.util.List;

import org.jboss.logging.Logger;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.InstanceMethodElementHandle;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;

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

    private IntrinsicBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate, Phase phase) {
        super(delegate);
        this.phase = phase;
        this.ctxt = ctxt;
    }

    public static IntrinsicBasicBlockBuilder createForAddPhase(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(ctxt, delegate, Phase.ADD);
    }

    public static IntrinsicBasicBlockBuilder createForAnalyzePhase(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(ctxt, delegate, Phase.ANALYZE);
    }

    public static IntrinsicBasicBlockBuilder createForLowerPhase(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(ctxt, delegate, Phase.LOWER);
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
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
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
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
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
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
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
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
        return super.invokeNoReturn(target, arguments, catchLabel);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
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
    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
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
        return super.tailInvoke(target, arguments, catchLabel);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle decodedTarget = (StaticMethodElementHandle) target;
            StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget.getExecutable());
            if (intrinsic != null) {
                Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                    (StaticMethodElementHandle) target,
                    arguments);
                goto_(resumeLabel);
                return result;
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
                goto_(resumeLabel);
                return result;
            }
        }
        return super.invoke(target, arguments, catchLabel, resumeLabel);
    }

}
