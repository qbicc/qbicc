package org.qbicc.plugin.vfs;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.VmThrowableClass;
import org.qbicc.machine.arch.Os;
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
    private final VmThrowableClass unsupportedOperationException;

    private VFS(CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ioException = (VmThrowableClass) classContext.findDefinedType("java/io/IOException").load().getVmClass();
        unsupportedOperationException = (VmThrowableClass) classContext.findDefinedType("java/lang/UnsupportedOperationException").load().getVmClass();
        vioSystem = VIO.get(ctxt).getVIOSystem();
        Os os = ctxt.getPlatform().os();
        fileSystem = os == Os.win32 ? new WindowsVirtualFileSystem(vioSystem) : new PosixVirtualFileSystem(vioSystem, os != Os.macos);
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
        vm.registerInvokable(hostIoDef.requireSingleMethod("readDirectoryEntry"), this::doHostReadDirectoryEntry);

        // UnixFileSystemProvider (NIO)

        LoadedTypeDefinition ufspDef = classContext.findDefinedType("sun/nio/fs/UnixFileSystemProvider").load();

        vm.registerInvokable(ufspDef.requireSingleMethod("checkAccess"), this::doUfspCheckAccess);
        vm.registerInvokable(ufspDef.requireSingleMethod("isRegularFile"), this::doUfspIsRegularFile);
        vm.registerInvokable(ufspDef.requireSingleMethod("isDirectory"), this::doUfspIsDirectory);
        vm.registerInvokable(ufspDef.requireSingleMethod("newDirectoryStream"), this::doUfspNewDirectoryStream);
        vm.registerInvokable(ufspDef.requireSingleMethod("exists"), this::doUfspExists);
        vm.registerInvokable(ufspDef.requireSingleMethod("readAttributes"), this::doUfspReadAttributes);
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
        return createBasicFileAttributes(vm, buf);
    }

    private Object doHostReadDirectoryEntry(final VmThread vmThread, final VmObject ignored, final List<Object> args) {
        //    public static native String readDirectoryEntry(int fd) throws IOException;
        int fd = ((Integer) args.get(0)).intValue();
        String entry;
        try {
            entry = fileSystem.getVioSystem().readDirectoryEntry(fd);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return entry == null ? null : vmThread.getVM().intern(entry);
    }

    private Object doUfspCheckAccess(final VmThread vmThread, final VmObject provider, final List<Object> args) {
        // Assume that if a file exists in the VFS, then it is readable
        return doUfspExists(vmThread, provider, args);
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

    private Object doUfspNewDirectoryStream(final VmThread vmThread, final VmObject ufsp, final List<Object> args) {
        String pathStr = toString(vmThread, (VmObject) args.get(0));
        VmObject filter = (VmObject) args.get(1);
        LoadedTypeDefinition hdsDef = ctxt.getBootstrapClassContext().findDefinedType("org/qbicc/runtime/host/HostDirectoryStream").load();
        return vmThread.getVM().newInstance(hdsDef.getVmClass(), hdsDef.requireSingleConstructor(
            ce -> ce.getParameters().get(0).getTypeDescriptor() instanceof ClassTypeDescriptor ctd
            && ctd.packageAndClassNameEquals("java/lang", "String")
        ), List.of(
            vmThread.getVM().intern(pathStr),
            filter
        ));
    }

    private Object doUfspExists(final VmThread vmThread, final VmObject ufsp, final List<Object> args) {
        String pathName = toString(vmThread, (VmObject) args.get(0));
        try {
            return Boolean.valueOf((fileSystem.getBooleanAttributes(fileSystem.getPath(pathName), true) & VFSUtils.BA_EXISTS) != 0);
        } catch (IOException e) {
            return Boolean.FALSE;
        }
    }

    private Object doUfspReadAttributes(final VmThread vmThread, final VmObject ufsp, final List<Object> args) {
        Vm vm = vmThread.getVM();
        String pathName = toString(vmThread, (VmObject) args.get(0));
        VmClass type = (VmClass) args.get(1);
        if (! type.getTypeDefinition().internalNameEquals("java/nio/file/attribute/BasicFileAttributes")) {
            // not supported during build
            throw new Thrown(unsupportedOperationException.newInstance("Only basic file attributes supported at build time"));
        }
        boolean followLinks = isFollowLinks(((VmReferenceArray) args.get(2)).getArray());
        VirtualFileStatBuffer buf;
        try {
            buf = fileSystem.stat(fileSystem.getPath(pathName), followLinks);
        } catch (IOException e) {
            throw wrapIOE(e);
        }
        return createBasicFileAttributes(vm, buf);
    }

    private boolean isFollowLinks(final VmObject[] options) {
        boolean followLinks = true;
        for (VmObject option : options) {
            if (option != null) {
                // there's only one option
                followLinks = false;
                break;
            }
        }
        return followLinks;
    }

    private VmObject createBasicFileAttributes(final Vm vm, final VirtualFileStatBuffer buf) {
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

    private String toString(final VmThread vmThread, final VmObject pathObj) {
        Vm vm = vmThread.getVM();
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        MethodDescriptor toStringDesc = MethodDescriptor.synthesize(classContext, stringDesc, List.of());
        MethodElement toStringMethod = pathObj.getVmClass().getTypeDefinition().resolveMethodElementVirtual(classContext, "toString", toStringDesc);
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
        if (e instanceof NoSuchFileException) {
            ClassContext classContext = ctxt.getBootstrapClassContext();
            return new Thrown(((VmThrowableClass) classContext.findDefinedType("java/nio/file/NoSuchFileException").load().getVmClass()).newInstance(e.getMessage()));
        } else {
            return new Thrown(ioException.newInstance(e.getMessage()));
        }
    }
}
