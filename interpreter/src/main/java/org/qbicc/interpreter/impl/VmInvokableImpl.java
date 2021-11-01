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
import org.qbicc.interpreter.InterpreterHaltedException;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmInvokable;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
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
    private final Schedule schedule;
    private final int memorySize;
    private volatile long count;

    VmInvokableImpl(ExecutableElement element) {
        this.element = element;
        int[] sizeHolder = new int[1];
        scheduled = buildScheduled(element, sizeHolder);
        schedule = element.getMethodBody().getSchedule();
        memorySize = sizeHolder[0];
    }

    private static Map<BasicBlock, List<Node>> buildScheduled(final ExecutableElement element, final int[] sizeHolder) {
        if (! element.tryCreateMethodBody()) {
            throw new IllegalStateException("No method body for " + element);
        }
        MethodBody body = element.getMethodBody();
        if (element.getEnclosingType().getContext().getCompilationContext().errors() > 0) {
            throw new InterpreterHaltedException("Interpreter halted due to compilation errors");
        }
        Map<BasicBlock, List<Node>> scheduled = new HashMap<>();
        buildScheduled(body, new HashSet<>(), scheduled, body.getEntryBlock().getTerminator(), sizeHolder);
        return scheduled;
    }

    private static void buildScheduled(final MethodBody body, final Set<Node> visited, final Map<BasicBlock, List<Node>> scheduled, Node node, int[] sizeHolder) {
        if (! visited.add(node)) {
            // already scheduled
            return;
        }
        if (node.hasValueHandleDependency()) {
            buildScheduled(body, visited, scheduled, node.getValueHandle(), sizeHolder);
        }
        if (node instanceof OrderedNode) {
            buildScheduled(body, visited, scheduled, ((OrderedNode) node).getDependency(), sizeHolder);
        }
        int cnt = node.getValueDependencyCount();
        for (int i = 0; i < cnt; i ++) {
            buildScheduled(body, visited, scheduled, node.getValueDependency(i), sizeHolder);
        }
        if (node instanceof Terminator) {
            // add outbound values
            Terminator terminator = (Terminator) node;
            Map<PhiValue, Value> outboundValues = terminator.getOutboundValues();
            for (PhiValue phiValue : outboundValues.keySet()) {
                buildScheduled(body, visited, scheduled, terminator.getOutboundValue(phiValue), sizeHolder);
            }
            // recurse to successors
            int sc = terminator.getSuccessorCount();
            for (int i = 0; i < sc; i ++) {
                BasicBlock successor = terminator.getSuccessor(i);
                buildScheduled(body, visited, scheduled, successor.getTerminator(), sizeHolder);
            }
        }
        if (node instanceof LocalVariable) {
            // reserve memory space
            LocalVariableElement varElem = ((LocalVariable) node).getVariableElement();
            ValueType varType = varElem.getType();
            int size = (int) varType.getSize();
            int align = varType.getAlign();
            if (align > 1) {
                int mask = align - 1;
                sizeHolder[0] = (sizeHolder[0] + mask) & ~mask;
            }
            varElem.setInterpreterOffset(sizeHolder[0]);
            sizeHolder[0] += size;
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
        return run((VmThreadImpl) thread, target, args);
    }

    Object run(VmThreadImpl thread, VmObject target, List<Object> args) {
        long invCnt = ((long) countHandle.getAndAdd(this, 1)) + 1;
        if (invCnt == 100) {
            thread.getVM().getCompilationContext().info(element, "Excessive invocation count (JIT candidate)");
        }
        if (! (element instanceof InitializerElement)) {
            ((VmClassImpl)element.getEnclosingType().load().getVmClass()).initialize(thread);
        }
        Frame caller = thread.currentFrame;
        Memory memory = thread.getVM().allocate(memorySize);
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
            thread.currentFrame = caller;
        }
    }
}
