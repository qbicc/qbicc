package org.qbicc.plugin.correctness;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

public class RuntimeInitManager {
    private static final AttachmentKey<RuntimeInitManager> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final ConcurrentHashMap<InitializerElement, VmObject> onceInstances = new ConcurrentHashMap<>();
    private int nextId = 1;

    private RuntimeInitManager(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static RuntimeInitManager get(CompilationContext ctxt) {
        RuntimeInitManager helper = ctxt.getAttachment(KEY);
        if (helper == null) {
            helper = new RuntimeInitManager(ctxt);
            RuntimeInitManager appearing = ctxt.putAttachmentIfAbsent(KEY, helper);
            if (appearing != null) {
                helper = appearing;
            }
        }
        return helper;
    }

    VmObject getOnceInstance(InitializerElement init) {
        VmObject obj = onceInstances.get(init);
        if (obj == null) {
            synchronized (this) {
                obj = onceInstances.get(init);
                if (obj == null) {
                    int thunkId = nextId++;
                    init.setLowerIndex(thunkId);
                    obj = allocateThunk(thunkId);
                    onceInstances.put(init, obj);
                }
            }
        }
        return obj;
    }

    private VmObject allocateThunk(int id) {
        MethodElement allocateThunk = RuntimeMethodFinder.get(ctxt).getMethod("org/qbicc/runtime/main/RuntimeInitializerRunner", "allocateThunk");
        return (VmObject)ctxt.getVm().invokeExact(allocateThunk, null, List.of(id));
    }

    public int maxAssignedId() {
        return nextId;
    }
}

