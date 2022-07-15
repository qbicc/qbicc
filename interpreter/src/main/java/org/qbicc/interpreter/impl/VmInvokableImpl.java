package org.qbicc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qbicc.graph.Action;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.Value;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmInvokable;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.LocalVariableElement;

/**
 *
 */
final class VmInvokableImpl implements VmInvokable {
    private static final VarHandle countHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "count", VarHandle.class, VmInvokableImpl.class, long.class);

    private final ExecutableElement element;
    private final Map<BasicBlock, List<Node>> scheduled;
    private final ValueType frameMemoryType;
    private final Schedule schedule;
    @SuppressWarnings("unused") // VarHandle
    private volatile long count;

    VmInvokableImpl(ExecutableElement element) {
        this.element = element;
        TypeSystem ts = element.getEnclosingType().getContext().getTypeSystem();
        final CompoundType.Builder builder = CompoundType.builder(ts);
        scheduled = buildScheduled(element, builder);
        if (builder.getMemberCountSoFar() == 0) {
            frameMemoryType = ts.getVoidType();
        } else {
            frameMemoryType = builder.build();
        }
        schedule = element.getMethodBody().getSchedule();
    }

    private static Map<BasicBlock, List<Node>> buildScheduled(final ExecutableElement element, CompoundType.Builder localVarLocations) {
        if (! element.tryCreateMethodBody()) {
            throw new IllegalStateException("No method body for " + element);
        }
        MethodBody body = element.getMethodBody();
        Map<BasicBlock, List<Node>> scheduled = new HashMap<>();
        buildScheduled(body, new HashSet<>(), scheduled, body.getEntryBlock().getTerminator(), localVarLocations);
        return scheduled;
    }

    private static void buildScheduled(final MethodBody body, final Set<Node> visited, final Map<BasicBlock, List<Node>> scheduled, Node node, CompoundType.Builder builder) {
        if (! visited.add(node)) {
            // already scheduled
            return;
        }
        if (node.hasValueHandleDependency()) {
            buildScheduled(body, visited, scheduled, node.getValueHandle(), builder);
        }
        if (node instanceof OrderedNode) {
            buildScheduled(body, visited, scheduled, ((OrderedNode) node).getDependency(), builder);
        }
        int cnt = node.getValueDependencyCount();
        for (int i = 0; i < cnt; i ++) {
            buildScheduled(body, visited, scheduled, node.getValueDependency(i), builder);
        }
        if (node instanceof Terminator terminator) {
            // add outbound values
            Map<PhiValue, Value> outboundValues = terminator.getOutboundValues();
            for (PhiValue phiValue : outboundValues.keySet()) {
                buildScheduled(body, visited, scheduled, terminator.getOutboundValue(phiValue), builder);
            }
            // recurse to successors
            int sc = terminator.getSuccessorCount();
            for (int i = 0; i < sc; i ++) {
                BasicBlock successor = terminator.getSuccessor(i);
                buildScheduled(body, visited, scheduled, successor.getTerminator(), builder);
            }
        }
        if (node instanceof LocalVariable lvn) {
            // reserve memory space
            LocalVariableElement varElem = lvn.getVariableElement();
            builder.addNextMember(varElem.getName(), varElem.getType());
            varElem.setOffset(builder.getLastAddedMember().getOffset());
        }
        if (! (node instanceof Terminator || node instanceof Unschedulable)) {
            // no need to explicitly add terminator since they're trivially findable and always last
            scheduled.computeIfAbsent(body.getSchedule().getBlockForNode(node), VmInvokableImpl::newList).add(node);
        }
    }

    private static List<Node> newList(final BasicBlock ignored) {
        return new ArrayList<>();
    }

    @Override
    public Object invokeAny(VmThread thread, VmObject target, List<Object> args) {
        VmThreadImpl threadImpl = (VmThreadImpl) thread;
        Thread old = threadImpl.getBoundThread();
        threadImpl.setBoundThread(Thread.currentThread());
        try {
            return run(threadImpl, target, args);
        } finally {
            threadImpl.setBoundThread(old);
        }
    }

    Object run(VmThreadImpl thread, VmObject target, List<Object> args) {
        long invCnt = ((long) countHandle.getAndAdd(this, 1)) + 1;
        if (false && invCnt == 100) {
            thread.getVM().getCompilationContext().info(element, "Excessive invocation count (JIT candidate)");
        }
        if (args.size() != element.getType().getParameterTypes().size()) {
            throw new Thrown(thread.vm.linkageErrorClass.newInstance("Parameter count mismatch"));
        }
        if (! (element instanceof InitializerElement)) {
            ((VmClassImpl)element.getEnclosingType().load().getVmClass()).initialize(thread);
        }
        Frame caller = thread.currentFrame;
        Memory memory = thread.getVM().allocate(frameMemoryType, 1);
        Frame frame = new Frame(caller, element, memory);
        thread.currentFrame = frame;
        // bind inputs
        MethodBody body = element.getMethodBody();
        if (! element.isStatic()) {
            frame.values.put(body.getThisValue(), target);
        }
        if (element instanceof InvokableElement) {
            for (int i = 0; i < args.size(); i++) {
                Object arg = args.get(i);
                // convenience
                if (arg instanceof String) {
                    arg = thread.getVM().manuallyInitialize(new VmStringImpl(thread.getVM(), thread.vm.stringClass, (String) arg));
                }
                try {
                    frame.values.put(body.getParameterValue(i), arg);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // for breakpoints
                    throw e;
                }
            }
        }
        try {
            frame.block = body.getEntryBlock();
            for (;;) {
                List<Node> nodes = scheduled.getOrDefault(frame.block, List.of());
                for (Node node : nodes) {
                    frame.ip = node;
                    if (frame.ip instanceof Value) {
                        Value value = (Value) frame.ip;
                        frame.values.put(value, value.accept(frame, thread));
                    } else {
                        assert frame.ip instanceof Action;
                        ((Action) frame.ip).accept(frame, thread);
                    }
                }
                Terminator t = frame.block.getTerminator();
                frame.ip = t;
                // keep it simple for now
                BasicBlock next = t.accept(frame, thread);
                if (next == null) {
                    // we're returning
                    return frame.output;
                }
                // register outbound phi values
                for (PhiValue phiValue : t.getOutboundValues().keySet()) {
                    // only register outbound values that will be used by the target
                    if (phiValue.getPinnedBlock() == next) {
                        if (schedule.getBlockForNode(phiValue) != null) {
                            // reachable value
                            Value value = t.getOutboundValue(phiValue);
                            Object realValue = frame.require(value);
                            frame.values.put(value, realValue);
                            frame.values.put(phiValue, realValue);
                        }
                    }
                }
                frame.block = next;
            }
        } catch (IllegalStateException | UnsupportedOperationException t) {
            // capture exception from frame state
            VmThrowableClassImpl internalErrorClass = (VmThrowableClassImpl) thread.vm.getBootstrapClassLoader().loadClass("java/lang/InternalError");
            throw new Thrown(internalErrorClass.newInstance("Internal error: " + t));
        } finally {
            frame.releaseLocks();
            thread.currentFrame = caller;
        }
    }
}
