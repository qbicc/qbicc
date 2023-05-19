package org.qbicc.interpreter.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.qbicc.graph.Action;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.DebugAddressDeclaration;
import org.qbicc.graph.DebugValueDeclaration;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.InvokeNoReturn;
import org.qbicc.graph.Node;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Value;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmInvokable;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.pointer.MemoryPointer;
import org.qbicc.type.StructType;
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
    private final ValueType frameMemoryType;
    @SuppressWarnings("unused") // VarHandle
    private volatile long count;

    VmInvokableImpl(ExecutableElement element) {
        this.element = element;
        TypeSystem ts = element.getEnclosingType().getContext().getTypeSystem();
        final StructType.Builder builder = StructType.builder(ts);
        // find local variables
        findLocalVars(element.getMethodBody().getEntryBlock().getTerminator(), builder, new HashSet<>(), new HashSet<>());
        if (builder.getMemberCountSoFar() == 0) {
            frameMemoryType = ts.getVoidType();
        } else {
            frameMemoryType = builder.build();
        }
    }

    private void findLocalVars(final Node node, final StructType.Builder builder, final Set<Node> visited, final Set<LocalVariableElement> found) {
        if (visited.add(node)) {
            // use the pointee or value type for the variable, because the LV type might
            // be array[0] but the pointee type will have the right dimension
            if (node instanceof DebugValueDeclaration dvd) {
                registerLocalVariable(builder, found, dvd.getVariable(), dvd.getValue().getType());
            } else if (node instanceof DebugAddressDeclaration dad) {
                registerLocalVariable(builder, found, dad.getVariable(), dad.getAddress().getPointeeType());
            }
            // debug decls are actions, so we only have to browse the action chain
            if (node instanceof OrderedNode on) {
                findLocalVars(on.getDependency(), builder, visited, found);
                if (on instanceof Terminator t) {
                    // then, recurse to successors
                    int sc = t.getSuccessorCount();
                    for (int i = 0; i < sc; i ++) {
                        findLocalVars(t.getSuccessor(i).getTerminator(), builder, visited, found);
                    }
                }
            }
        }
    }

    private static void registerLocalVariable(final StructType.Builder builder, final Set<LocalVariableElement> found, final LocalVariableElement lve, ValueType lveType) {
        if (found.add(lve)) {
            builder.addNextMember(lve.getName(), lveType);
            lve.setOffset(builder.getLastAddedMember().getOffset());
        }
    }

    @Override
    public Object invokeAny(VmThread thread, VmObject target, List<Object> args) {
        VmThreadImpl threadImpl = (VmThreadImpl) thread;
        threadImpl.setBoundThread(Thread.currentThread());
        return run(threadImpl, target, args);
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
        Frame frame = new Frame(caller, element, new MemoryPointer(frameMemoryType.getPointer(), memory));
        thread.currentFrame = frame;
        // bind inputs
        MethodBody body = element.getMethodBody();
        if (! element.isStatic()) {
            BlockParameter bp = body.getEntryBlock().getBlockParameter(Slot.this_());
            if (bp != null) {
                frame.values.put(bp, target);
            }
        }
        if (element instanceof InvokableElement) {
            for (int i = 0; i < args.size(); i++) {
                Object arg = args.get(i);
                // convenience
                if (arg instanceof String) {
                    arg = thread.getVM().manuallyInitialize(new VmStringImpl(thread.getVM(), thread.vm.stringClass, (String) arg));
                }
                BlockParameter bp = body.getEntryBlock().getBlockParameter(body.getParameterSlot(i));
                if (bp != null) {
                    frame.values.put(bp, arg);
                }
            }
        }
        try {
            frame.block = body.getEntryBlock();
            ArrayDeque<BasicBlock> prev = new ArrayDeque<>(20) {
                @Override
                public void addLast(BasicBlock basicBlock) {
                    if (size() == 20) {
                        removeFirst();
                    }
                    super.addLast(basicBlock);
                }
            };
            for (;;) {
                prev.addLast(frame.block); // for debugging
                List<Node> nodes = frame.block.getInstructions();
                for (Node node : nodes) {
                    frame.ip = node;
                    // only execute values and actions; terminators are handled below
                    if (frame.ip instanceof Value value) {
                        frame.values.put(value, value.accept(frame, thread));
                    } else if (frame.ip instanceof Action action) {
                        action.accept(frame, thread);
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
                // register outbound values
                for (Slot slot : t.getOutboundArgumentNames()) {
                    BlockParameter param = next.getBlockParameter(slot);
                    if (param != null) {
                        Value value = t.getOutboundArgument(slot);
                        frame.values.put(param, frame.require(value));
                    }
                }
                // special: Invoke
                if (t instanceof Invoke inv) {
                    if (next == inv.getResumeTarget()) {
                        BlockParameter bp = next.getBlockParameter(Slot.result());
                        if (bp != null) {
                            // todo: frame.returnVal maybe
                            frame.values.put(bp, frame.require(inv.getReturnValue()));
                        }
                    } else {
                        assert next == inv.getCatchBlock();
                        BlockParameter bp = next.getBlockParameter(Slot.thrown());
                        if (bp != null) {
                            frame.values.put(bp, frame.exception);
                        }
                        frame.exception = null;
                    }
                } else if (t instanceof InvokeNoReturn inv) {
                    assert next == inv.getCatchBlock();
                    BlockParameter bp = next.getBlockParameter(Slot.thrown());
                    if (bp != null) {
                        frame.values.put(bp, frame.exception);
                    }
                    frame.exception = null;
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
