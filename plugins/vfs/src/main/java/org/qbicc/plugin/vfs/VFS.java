package org.qbicc.plugin.vfs;

import java.io.IOException;
import java.util.List;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.VmThrowableClass;
import org.qbicc.machine.arch.OS;
import org.qbicc.machine.vfs.AbsoluteVirtualPath;
import org.qbicc.machine.vfs.PosixVirtualFileSystem;
import org.qbicc.machine.vfs.VFSUtils;
import org.qbicc.machine.vfs.VirtualFileStatBuffer;
import org.qbicc.machine.vfs.VirtualFileSystem;
import org.qbicc.machine.vfs.VirtualPath;
import org.qbicc.machine.vfs.WindowsVirtualFileSystem;
import org.qbicc.machine.vio.VIOSystem;
import org.qbicc.plugin.vio.VIO;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

public final class VFS {
    private static final AttachmentKey<VFS> KEY = new AttachmentKey<>();
    private static final AttachmentKey<List<VirtualPath>> CLASS_PATH_KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final VirtualFileSystem fileSystem;
    private final AbsoluteVirtualPath qbiccPath;
    private final VIOSystem vioSystem;
    private final VmThrowableClass ioException;

    private VFS(CompilationContext ctxt) {
        this.ctxt = ctxt;
        ioException = (VmThrowableClass) ctxt.getBootstrapClassContext().findDefinedType("java/io/IOException").load().getVmClass();
        vioSystem = VIO.get(ctxt).getVIOSystem();
        OS os = ctxt.getPlatform().getOs();
        fileSystem = os == OS.WIN32 ? new WindowsVirtualFileSystem(vioSystem) : new PosixVirtualFileSystem(vioSystem, os != OS.DARWIN);
        qbiccPath = fileSystem.getPath("/qbicc").toAbsolutePath();
    }

    @SuppressWarnings("OctalInteger")
    public static VFS initialize(CompilationContext ctxt) {
        VFS attachment = ctxt.getAttachment(KEY);
        if (attachment != null) {
            throw new IllegalStateException();
        }
        attachment = new VFS(ctxt);
        VFS appearing = ctxt.putAttachmentIfAbsent(KEY, attachment);
        if (appearing != null) {
            throw new IllegalStateException();
        }
        // one-time setup
        attachment.registerInvokables();
        try {
            // set up initial filesystem
            attachment.fileSystem.mkdirs(attachment.qbiccPath, 0755);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return attachment;
    }

    public static VFS get(CompilationContext ctxt) {
        VFS attachment = ctxt.getAttachment(KEY);
        if (attachment == null) {
            throw new IllegalStateException();
        }
        return attachment;
    }

    private void registerInvokables() {
        Vm vm = ctxt.getVm();

        ClassContext classContext = ctxt.getBootstrapClassContext();

        // HostIO (VFS aspects; VIO methods can be found in org.qbicc.plugin.vio.VIO)

        LoadedTypeDefinition hostIoDef = classContext.findDefinedType("org/qbicc/runtime/host/HostIO").load();

        vm.registerInvokable(hostIoDef.requireSingleMethod("open"), this::doHostOpen);
        vm.registerInvokable(hostIoDef.requireSingleMethod("reopen"), this::doHostReopen);
        vm.registerInvokable(hostIoDef.requireSingleMethod("mkdir"), this::doHostMkdir);
        vm.registerInvokable(hostIoDef.requireSingleMethod("unlink"), this::doHostUnlink);
        vm.registerInvokable(hostIoDef.requireSingleMethod("getBooleanAttributes"), this::doHostGetBooleanAttributes);
        vm.registerInvokable(hostIoDef.requireSingleMethod("stat"), this::doHostStat);

        // UnixFileSystemProvider (NIO)

        LoadedTypeDefinition ufspDef = classContext.findDefinedType("sun/nio/fs/UnixFileSystemProvider").load();

        vm.registerInvokable(ufspDef.requireSingleMethod("isRegularFile"), this::doUfspIsRegularFile);
        vm.registerInvokable(ufspDef.requireSingleMethod("isDirectory"), this::doUfspIsDirectory);
    }

    private Object doHostOpen(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        String pathName = ((VmString)args.get(0)).getContent();
        int openFlags = ((Integer) args.get(1)).intValue();
        int mode = ((Integer) args.get(2)).intValue();
        int fd;
        try {
            fd = fileSystem.open(fileSystem.getPath(pathName), openFlags, mode);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return Integer.valueOf(fd);
    }

    private Object doHostReopen(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        int fd = ((Integer) args.get(0)).intValue();
        String pathName = ((VmString)args.get(1)).getContent();
        int openFlags = ((Integer) args.get(2)).intValue();
        int mode = ((Integer) args.get(3)).intValue();
        try {
            fileSystem.open(fd, fileSystem.getPath(pathName), openFlags, mode);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return null;
    }

    private Object doHostMkdir(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        String pathName = ((VmString)args.get(0)).getContent();
        int mode = ((Integer) args.get(1)).intValue();
        try {
            fileSystem.mkdir(fileSystem.getPath(pathName), mode);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return null;
    }

    private Object doHostUnlink(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        String pathName = ((VmString)args.get(0)).getContent();
        try {
            fileSystem.unlink(fileSystem.getPath(pathName));
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return null;
    }

    private Object doHostGetBooleanAttributes(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        String pathName = ((VmString)args.get(0)).getContent();
        int val;
        try {
            val = fileSystem.getBooleanAttributes(fileSystem.getPath(pathName), true);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return Integer.valueOf(val);
    }

    private Object doHostStat(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        String pathName = ((VmString)args.get(0)).getContent();
        boolean followLinks = ((Boolean) args.get(1)).booleanValue();
        VirtualFileStatBuffer buf;
        try {
            buf = fileSystem.stat(fileSystem.getPath(pathName), followLinks);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        Vm vm = vmThread.getVM();
        VmArray vmArray = vm.newLongArray(new long[] {
            buf.getModTime(),
            buf.getAccessTime(),
            buf.getCreateTime(),
            buf.getBooleanAttributes(),
            buf.getSize(),
            buf.getFileId()
        });
        LoadedTypeDefinition hbfaDef = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/host/HostBasicFileAttributes").load();
        VmClass hbfaClass = hbfaDef.getVmClass();
        return vm.newInstance(hbfaClass, hbfaDef.requireSingleConstructor(ce -> true), List.of(vmArray));
    }

    private Object doUfspIsRegularFile(final VmThread vmThread, final VmObject provider, final List<Object> args) {
        String pathStr = toString(vmThread, (VmObject) args.get(0));
        boolean result;
        try {
            result = (fileSystem.getBooleanAttributes(fileSystem.getPath(pathStr), true) & VFSUtils.BA_REGULAR) != 0;
        } catch (IOException e) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(result);
    }

    private Object doUfspIsDirectory(final VmThread vmThread, final VmObject provider, final List<Object> args) {
        String pathStr = toString(vmThread, (VmObject) args.get(0));
        boolean result;
        try {
            result = (fileSystem.getBooleanAttributes(fileSystem.getPath(pathStr), true) & VFSUtils.BA_DIRECTORY) != 0;
        } catch (IOException e) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(result);
    }

    private String toString(final VmThread vmThread, final VmObject pathObj) {
        Vm vm = vmThread.getVM();
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        MethodDescriptor toStringDesc = MethodDescriptor.synthesize(classContext, stringDesc, List.of());
        MethodElement toStringMethod = pathObj.getVmClass().getTypeDefinition().resolveMethodElementVirtual("toString", toStringDesc);
        VmString str = (VmString) vm.invokeExact(toStringMethod, pathObj, List.of());
        return str.getContent();
    }


    public static List<VirtualPath> getClassPathEntries(final CompilationContext ctxt) {
        return ctxt.getAttachment(CLASS_PATH_KEY);
    }

    public static void attachClassPath(final CompilationContext ctxt, List<VirtualPath> paths) {
        ctxt.putAttachment(CLASS_PATH_KEY, paths);
    }

    public VirtualFileSystem getFileSystem() {
        return fileSystem;
    }

    public AbsoluteVirtualPath getQbiccPath() {
        return qbiccPath;
    }

    private Thrown wrapIOE(final IOException e) {
        return new Thrown(ioException.newInstance(e.getMessage()));
    }
}
