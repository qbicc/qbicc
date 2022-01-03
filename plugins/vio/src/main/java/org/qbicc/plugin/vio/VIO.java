package org.qbicc.plugin.vio;

import java.util.List;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * Virtual I/O.
 */
public final class VIO {
    private static final AttachmentKey<VIO> KEY = new AttachmentKey<>();
    private final CompilationContext ctxt;

    private VIO(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static VIO get(CompilationContext ctxt) {
        VIO instance = ctxt.getAttachment(KEY);
        if (instance == null) {
            instance = new VIO(ctxt);
            VIO appearing = ctxt.putAttachmentIfAbsent(KEY, instance);
            if (appearing != null) {
                instance = appearing;
            } else {
                instance.registerInvokables();
            }
        }
        return instance;
    }

    private void registerInvokables() {
        Vm vm = ctxt.getVm();

        ClassContext classContext = ctxt.getBootstrapClassContext();

        // UNIX

        LoadedTypeDefinition ioUnixFileSystemDef = vm.loadClass(classContext, "java/io/UnixFileSystem").load();

        vm.registerInvokable(ioUnixFileSystemDef.requireSingleMethod(me -> me.nameEquals("checkAccess")), this::doCheckAccess);
        vm.registerInvokable(ioUnixFileSystemDef.requireSingleMethod(me -> me.nameEquals("getBooleanAttributes0")), this::doGetBooleanAttributes);
    }

    private Object doCheckAccess(final VmThread vmThread, final VmObject vmObject, final List<Object> args) {
        // todo: vfs
        return Boolean.FALSE;
    }

    private Object doGetBooleanAttributes(final VmThread vmThread, final VmObject vmObject, final List<Object> args) {
        // todo: vfs
        return Integer.valueOf(0);
    }

}
