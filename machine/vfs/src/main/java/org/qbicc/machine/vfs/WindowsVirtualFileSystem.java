package org.qbicc.machine.vfs;

import java.io.IOException;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Locale;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.SortedMaps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharObjectMap;
import org.eclipse.collections.api.map.sorted.ImmutableSortedMap;
import org.eclipse.collections.impl.factory.primitive.CharObjectMaps;
import org.qbicc.machine.vio.VIOSystem;

/**
 *
 */
public class WindowsVirtualFileSystem extends VirtualFileSystem {
    private static final VarHandle cachedDriveLettersHandle = ConstantBootstraps.fieldVarHandle(
        MethodHandles.lookup(),
        "cachedDriveLetters",
        VarHandle.class,
        WindowsVirtualFileSystem.class,
        ImmutableCharObjectMap.class
    );
    private static final VarHandle cachedUncHostsHandle = ConstantBootstraps.fieldVarHandle(
        MethodHandles.lookup(),
        "cachedUncHosts",
        VarHandle.class,
        WindowsVirtualFileSystem.class,
        ImmutableMap.class
    );
    private static final VarHandle rootsHandle = ConstantBootstraps.fieldVarHandle(
        MethodHandles.lookup(),
        "roots",
        VarHandle.class,
        WindowsVirtualFileSystem.class,
        ImmutableSortedMap.class
    );

    private final char defaultDriveLetter;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile ImmutableCharObjectMap<DriveLetterVirtualRootName> cachedDriveLetters;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile ImmutableMap<String, UncVirtualRootName> cachedUncHosts = Maps.immutable.empty();

    @SuppressWarnings("FieldMayBeFinal")
    private volatile ImmutableSortedMap<VirtualRootName, VirtualRoot> roots;

    public WindowsVirtualFileSystem(VIOSystem vioSystem) {
        this(vioSystem, 'C');
    }

    public WindowsVirtualFileSystem(VIOSystem vioSystem, char defaultDriveLetter) {
        super(vioSystem, String::compareToIgnoreCase);
        this.defaultDriveLetter = Character.toUpperCase(defaultDriveLetter);
        DriveLetterVirtualRootName defaultRootName = new DriveLetterVirtualRootName(this, defaultDriveLetter);
        cachedDriveLetters = CharObjectMaps.immutable.of(defaultDriveLetter, defaultRootName);
        roots = SortedMaps.immutable.of(defaultRootName, new VirtualRoot(defaultRootName, this));
    }

    @Override
    public VirtualRootName getDefaultRootName() {
        return cachedDriveLetters.get(defaultDriveLetter);
    }

    @Override
    public String getSeparator() {
        return "\\";
    }

    @Override
    public List<AbsoluteVirtualPath> getRootDirectories() {
        return roots.collect(item -> item.getRootName().getRootPath()).toList();
    }

    public DriveLetterVirtualRootName getDriveLetterRootName(char driveLetter) {
        driveLetter = Character.toUpperCase(driveLetter);
        if (driveLetter < 'A' || 'Z' < driveLetter) {
            throw new IllegalArgumentException();
        }
        ImmutableCharObjectMap<DriveLetterVirtualRootName> oldVal, newVal;
        oldVal = this.cachedDriveLetters;
        DriveLetterVirtualRootName rootName = oldVal.get(driveLetter);
        if (rootName != null) {
            return rootName;
        }
        DriveLetterVirtualRootName added = new DriveLetterVirtualRootName(this, driveLetter);
        newVal = oldVal.newWithKeyValue(driveLetter, added);
        while (! cachedDriveLettersHandle.compareAndSet(this, oldVal, newVal)) {
            oldVal = this.cachedDriveLetters;
            rootName = oldVal.get(driveLetter);
            if (rootName != null) {
                return rootName;
            }
            newVal = oldVal.newWithKeyValue(driveLetter, added);
        }
        return added;
    }

    public UncVirtualRootName getUncVirtualRootName(String hostName) {
        hostName = hostName.toUpperCase(Locale.ROOT);
        // todo validate the actual string itself
        ImmutableMap<String, UncVirtualRootName> oldVal, newVal;
        oldVal = this.cachedUncHosts;
        UncVirtualRootName rootName = oldVal.get(hostName);
        if (rootName != null) {
            return rootName;
        }
        UncVirtualRootName added = new UncVirtualRootName(this, hostName);
        newVal = oldVal.newWithKeyValue(hostName, added);
        while (! cachedUncHostsHandle.compareAndSet(this, oldVal, newVal)) {
            oldVal = this.cachedUncHosts;
            rootName = oldVal.get(hostName);
            if (rootName != null) {
                return rootName;
            }
            newVal = oldVal.newWithKeyValue(hostName, added);
        }
        return added;
    }

    public VirtualRoot getDrive(char driveLetter) {
        return get(getDriveLetterRootName(driveLetter));
    }

    public VirtualRoot get(VirtualRootName rootName) {
        return roots.get(rootName);
    }

    public VirtualRoot getOrMount(VirtualRootName rootName) {
        Assert.checkNotNullParam("rootName", rootName);
        ImmutableSortedMap<VirtualRootName, VirtualRoot> oldVal, newVal;
        oldVal = this.roots;
        VirtualRoot virtualRoot = oldVal.get(rootName);
        if (virtualRoot != null) {
            return virtualRoot;
        }
        VirtualRoot added = new VirtualRoot(rootName, this);
        newVal = oldVal.newWithKeyValue(rootName, added);
        while (! rootsHandle.compareAndSet(this, oldVal, newVal)) {
            oldVal = this.roots;
            virtualRoot = oldVal.get(rootName);
            if (virtualRoot != null) {
                return virtualRoot;
            }
            newVal = oldVal.newWithKeyValue(rootName, added);
        }
        return added;
    }

    public VirtualPath getPath(String first, String... rest) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Windows");
    }

    @Override
    DirectoryNode getRootNode(VirtualRootName rootName) {
        VirtualRoot virtualRoot = roots.get(rootName);
        return virtualRoot == null ? null : virtualRoot.getRootDirectory();
    }

    @Override
    public boolean isHidden(VirtualPath path) throws IOException {
        // todo
        return false;
    }
}
