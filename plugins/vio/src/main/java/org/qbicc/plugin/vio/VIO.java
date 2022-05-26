package org.qbicc.plugin.vio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.VmThrowableClass;
import org.qbicc.machine.vio.VIOSystem;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * Virtual I/O.
 */
public final class VIO {
    private static final AttachmentKey<VIO> KEY = new AttachmentKey<>();
    private final CompilationContext ctxt;
    private final VIOSystem system;
    private final VmThrowableClass ioException;

    private VIO(CompilationContext ctxt) {
        this.ctxt = ctxt;
        // todo: configurable max fd
        system = new VIOSystem(256);
        ioException = (VmThrowableClass) ctxt.getBootstrapClassContext().findDefinedType("java/io/IOException").load().getVmClass();
        try {
            // todo: configurable build-time process input source
            system.openInputStream(0, InputStream::nullInputStream);
            // todo: capture output and error streams
            system.openOutputStream(1, OutputStream::nullOutputStream);
            system.openOutputStream(2, OutputStream::nullOutputStream);
        } catch (IOException e) {
            // should be impossible
            throw new RuntimeException(e);
        }
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

    public VIOSystem getVIOSystem() {
        return system;
    }

    private void registerInvokables() {
        Vm vm = ctxt.getVm();

        ClassContext classContext = ctxt.getBootstrapClassContext();

        // HostIO (VIO aspects; VFS methods can be found in org.qbicc.plugin.vfs.VFS)

        LoadedTypeDefinition hostIoDef = classContext.findDefinedType("org/qbicc/runtime/host/HostIO").load();

        vm.registerInvokable(hostIoDef.requireSingleMethod("close"), this::doHostClose);
        vm.registerInvokable(hostIoDef.requireSingleMethod("pipe"), this::doHostPipe);
        vm.registerInvokable(hostIoDef.requireSingleMethod("socketpair"), this::doHostSocketPair);
        vm.registerInvokable(hostIoDef.requireSingleMethod("dup"), this::doHostDup);
        vm.registerInvokable(hostIoDef.requireSingleMethod("dup2"), this::doHostDup2);
        vm.registerInvokable(hostIoDef.requireSingleMethod("read"), this::doHostRead);
        vm.registerInvokable(hostIoDef.requireSingleMethod("readSingle"), this::doHostReadSingle);
        vm.registerInvokable(hostIoDef.requireSingleMethod("available"), this::doHostAvailable);
        vm.registerInvokable(hostIoDef.requireSingleMethod("write"), this::doHostWrite);
        vm.registerInvokable(hostIoDef.requireSingleMethod("writeSingle"), this::doHostWriteSingle);
        vm.registerInvokable(hostIoDef.requireSingleMethod("getFileSize"), this::doHostGetFileSize);
        vm.registerInvokable(hostIoDef.requireSingleMethod("seekAbsolute"), this::doHostSeekAbsolute);
        vm.registerInvokable(hostIoDef.requireSingleMethod("seekRelative"), this::doHostSeekRelative);
    }

    private Object doHostClose(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        try {
            system.close(fd);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return null;
    }

    private Object doHostPipe(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int[] fds;
        try {
            fds = system.pipe();
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return vmThread.getVM().newIntArray(fds);
    }

    private Object doHostSocketPair(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int[] fds;
        try {
            fds = system.socketPair();
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return vmThread.getVM().newIntArray(fds);
    }

    private Object doHostDup(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        int res;
        try {
            res = system.dup(fd);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return Integer.valueOf(res);
    }

    private Object doHostDup2(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int oldFd = ((Integer) args.get(0)).intValue();
        int newFd = ((Integer) args.get(1)).intValue();
        try {
            system.dup2(oldFd, newFd);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return null;
    }

    private Object doHostRead(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        byte[] array = (byte[]) ((VmArray) args.get(1)).getArray();
        int off = ((Integer) args.get(2)).intValue();
        int len = ((Integer) args.get(3)).intValue();
        int res;
        try {
            res = system.read(fd, ByteBuffer.wrap(array, off, len));
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return Integer.valueOf(res);
    }

    private Object doHostReadSingle(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        int res;
        try {
            res = system.readSingle(fd);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return Integer.valueOf(res);
    }

    private Object doHostAvailable(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        long res;
        try {
            res = system.available(fd);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return Long.valueOf(res);
    }

    private Object doHostWrite(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        byte[] array = (byte[]) ((VmArray) args.get(1)).getArray();
        int off = ((Integer) args.get(2)).intValue();
        int len = ((Integer) args.get(3)).intValue();
        int res;
        try {
            res = system.write(fd, ByteBuffer.wrap(array, off, len));
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return Integer.valueOf(res);
    }

    private Object doHostWriteSingle(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        int val = ((Integer) args.get(0)).intValue() & 0xff;
        try {
            system.writeSingle(fd, val);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return null;
    }

    private Object doHostGetFileSize(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        try {
            return Long.valueOf(system.getFileSize(fd));
        } catch (IOException e) {
            throw wrapIOE(e);
        }
    }

    private Object doHostSeekAbsolute(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        long offs = ((Long) args.get(1)).longValue();
        try {
            return Long.valueOf(system.seekAbsolute(fd, offs));
        } catch (IOException e) {
            throw wrapIOE(e);
        }
    }

    private Object doHostSeekRelative(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        long offs = ((Long) args.get(1)).longValue();
        try {
            return Long.valueOf(system.seekRelative(fd, offs));
        } catch (IOException e) {
            throw wrapIOE(e);
        }
    }

    private Thrown wrapIOE(final IOException e) {
        return new Thrown(ioException.newInstance(e.getMessage()));
    }
}
