package org.qbicc.plugin.intrinsics;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.type.definition.element.MethodElement;
import org.jboss.logging.Logger;

/**
 * The basic block builder which substitutes invocations of intrinsic methods.
 * 
 * This only handles the "unresolved" form of method calls and assumes that all
 * methods to be replaced by intrinsics originate from descriptors (ie: classfile
 * parsing).
 */
public final class IntrinsicBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, MethodElement> {
    public static final Logger log = Logger.getLogger("org.qbicc.plugin.intrinsics");

    private static final ValueHandleVisitor<Void, MethodElement> GET_METHOD = new ValueHandleVisitor<Void, MethodElement>() {
        @Override
        public MethodElement visit(Void param, ExactMethodElementHandle node) {
            return node.getExecutable();
        }

        @Override
        public MethodElement visit(Void param, InterfaceMethodElementHandle node) {
            return node.getExecutable();
        }

        @Override
        public MethodElement visit(Void param, VirtualMethodElementHandle node) {
            return node.getExecutable();
        }

        @Override
        public MethodElement visit(Void param, StaticMethodElementHandle node) {
            return node.getExecutable();
        }
    };
    private static final ValueHandleVisitor<Void, Value> GET_INSTANCE = new ValueHandleVisitor<Void, Value>() {
        @Override
        public Value visit(Void param, ExactMethodElementHandle node) {
            return node.getInstance();
        }

        @Override
        public Value visit(Void param, InterfaceMethodElementHandle node) {
            return node.getInstance();
        }

        @Override
        public Value visit(Void param, VirtualMethodElementHandle node) {
            return node.getInstance();
        }
    };

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

    public static IntrinsicBasicBlockBuilder createForLowerPhase(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        return new IntrinsicBasicBlockBuilder(ctxt, delegate, Phase.LOWER);
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        MethodElement decodedTarget = target.accept(GET_METHOD, null);
        if (decodedTarget != null) {
            if (decodedTarget.isStatic()) {
                StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        decodedTarget,
                        arguments);
                    if (result != null) {
                        return result;
                    }
                }
            } else {
                // instance
                InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value instance = target.accept(GET_INSTANCE, null);
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        instance,
                        decodedTarget,
                        arguments);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return super.call(target, arguments);
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        MethodElement decodedTarget = target.accept(this, null);
        if (decodedTarget != null) {
            if (decodedTarget.isStatic()) {
                StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        decodedTarget,
                        arguments);
                    if (result != null) {
                        return result;
                    }
                }
            } else {
                // instance
                InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value instance = target.accept(GET_INSTANCE, null);
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        instance,
                        decodedTarget,
                        arguments);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return super.callNoSideEffects(target, arguments);
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        MethodElement decodedTarget = target.accept(this, null);
        if (decodedTarget != null) {
            if (decodedTarget.isStatic()) {
                StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    intrinsic.emitIntrinsic(getFirstBuilder(),
                        decodedTarget,
                        arguments);
                    return unreachable();
                }
            } else {
                // instance
                InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value instance = target.accept(GET_INSTANCE, null);
                    intrinsic.emitIntrinsic(getFirstBuilder(),
                        instance,
                        decodedTarget,
                        arguments);
                    return unreachable();
                }
            }
        }
        return super.callNoReturn(target, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        MethodElement decodedTarget = target.accept(this, null);
        if (decodedTarget != null) {
            if (decodedTarget.isStatic()) {
                StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    intrinsic.emitIntrinsic(getFirstBuilder(),
                        decodedTarget,
                        arguments);
                    return unreachable();
                }
            } else {
                // instance
                InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value instance = target.accept(GET_INSTANCE, null);
                    intrinsic.emitIntrinsic(getFirstBuilder(),
                        instance,
                        decodedTarget,
                        arguments);
                    return unreachable();
                }
            }
        }
        return super.invokeNoReturn(target, arguments, catchLabel);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        MethodElement decodedTarget = target.accept(this, null);
        if (decodedTarget != null) {
            if (decodedTarget.isStatic()) {
                StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        decodedTarget,
                        arguments);
                    if (result != null) {
                        return return_(result);
                    }
                }
            } else {
                // instance
                InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value instance = target.accept(GET_INSTANCE, null);
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        instance,
                        decodedTarget,
                        arguments);
                    if (result != null) {
                        return return_(result);
                    }
                }
            }
        }
        return super.tailCall(target, arguments);
    }

    @Override
    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        MethodElement decodedTarget = target.accept(this, null);
        if (decodedTarget != null) {
            if (decodedTarget.isStatic()) {
                StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        decodedTarget,
                        arguments);
                    if (result != null) {
                        return return_(result);
                    }
                }
            } else {
                // instance
                InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value instance = target.accept(GET_INSTANCE, null);
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        instance,
                        decodedTarget,
                        arguments);
                    if (result != null) {
                        return return_(result);
                    }
                }
            }
        }
        return super.tailInvoke(target, arguments, catchLabel);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        MethodElement decodedTarget = target.accept(GET_METHOD, null);
        if (decodedTarget != null) {
            if (decodedTarget.isStatic()) {
                StaticIntrinsic intrinsic = Intrinsics.get(ctxt).getStaticIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        decodedTarget,
                        arguments);
                    goto_(resumeLabel);
                    return result;
                }
            } else {
                // instance
                InstanceIntrinsic intrinsic = Intrinsics.get(ctxt).getInstanceIntrinsic(phase, decodedTarget);
                if (intrinsic != null) {
                    Value instance = target.accept(GET_INSTANCE, null);
                    Value result = intrinsic.emitIntrinsic(getFirstBuilder(),
                        instance,
                        decodedTarget,
                        arguments);
                    goto_(resumeLabel);
                    return result;
                }
            }
        }
        return super.invoke(target, arguments, catchLabel, resumeLabel);
    }

}
