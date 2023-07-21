package org.qbicc.plugin.llvm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.driver.Driver;
import org.qbicc.graph.InvocationNode;
import org.qbicc.graph.Node;
import org.qbicc.machine.llvm.CallingConvention;
import org.qbicc.machine.llvm.stackmap.LocationType;
import org.qbicc.machine.llvm.stackmap.StackMap;
import org.qbicc.machine.llvm.stackmap.StackMapVisitor;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.object.Function;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.plugin.methodinfo.CallSiteTable;
import org.qbicc.plugin.methodinfo.valueinfo.FrameOffsetValueInfo;
import org.qbicc.plugin.methodinfo.valueinfo.RegisterRelativeValueInfo;
import org.qbicc.plugin.methodinfo.valueinfo.RegisterValueInfo;
import org.qbicc.plugin.methodinfo.valueinfo.ValueInfo;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * A utility to collect stack map data from an object file produced from LLVM which includes statepoint calls.
 * <p>
 * Note that statepoint uses the stack map format slightly differently than described in the stack map documentation.
 * Specifically, each stack map starts with three constant entries which give information about the usage site,
 * and there are two consecutive entries for each actual variable (one corresponding to the base pointer and one for
 * the derived pointer, which in our case is presently always the same as the base pointer).
 */
public final class LLVMStackMapCollector {
    private final CompilationContext ctxt;

    public LLVMStackMapCollector(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public void collect() {
        Linker linker = Linker.get(ctxt);
        LLVMInfo info = LLVMInfo.get(ctxt);
        ObjectFileProvider objFileProvider = ctxt.getAttachment(Driver.OBJ_PROVIDER_TOOL_KEY);
        Iterator<Map.Entry<LoadedTypeDefinition, Path>> objFileIterator = linker.getObjectFilePathsWithTypeInLinkOrder().iterator();
        final CallSiteTable cst = CallSiteTable.get(ctxt);

        ctxt.runParallelTask(ctxt -> {
            Map.Entry<LoadedTypeDefinition, Path> entry;
            for (; ; ) {
                synchronized (objFileIterator) {
                    if (!objFileIterator.hasNext()) {
                        return;
                    }
                    entry = objFileIterator.next();
                }
                final LoadedTypeDefinition typeDefinition = entry.getKey();
                final List<InvocationNode> callSitesById = info.getStatePointIds(typeDefinition);
                if (callSitesById == null) {
                    throw new IllegalStateException("Missing statepoint IDs");
                }
                final Path objFile = entry.getValue();
                try (ObjectFile objectFile = objFileProvider.openObjectFile(objFile)) {
                    org.qbicc.machine.object.Section stackMapSection = objectFile.getSection(objectFile.getStackMapSectionName());
                    if (stackMapSection != null) {
                        ByteBuffer stackMapData = stackMapSection.getSectionContent();
                        StackMap.parse(stackMapData, new StackMapVisitor() {

                            // per-unit
                            private final List<CallSiteTable.CallSiteEntry> callSites = new ArrayList<>();

                            // per-function
                            private Function functionAddress;
                            private CallSiteTable.SourceCodeEntry sc;
                            private long stackSize;
                            private final List<CallSiteTable.CallSiteEntry> fnCallSites = new ArrayList<>();

                            // per-call-site
                            private CallingConvention cconv;
                            private long offset;
                            private CallSiteTable.SubprogramEntry se;
                            private final HashSet<ValueInfo> valueInfos = new HashSet<>();

                            public void start(int version, long fnCount, long recCount) {
                                if (version != 3) {
                                    ctxt.error(Location.builder().setSourceFilePath(objectFile.toString()).build(), "Stack map version %d not supported", Integer.valueOf(version));
                                }
                            }

                            public void startFunction(long fnIndex, long address, long stackSize, long recordCount) {
                                // todo: Replace the `address` argument with a Literal which represents the relocation with offset;
                                // the address is actually a relocation... but we can cheat and just grab the function itself by index
                                functionAddress = ctxt.getOrAddProgramModule(typeDefinition).getFunction((int) fnIndex);
                                this.stackSize = stackSize;
                            }

                            public void startRecord(long recIndex, long patchPointId, long offset, int locCnt, int liveOutCnt) {
                                this.offset = offset;
                                final Node node = callSitesById.get(Math.toIntExact(patchPointId));
                                se = cst.getSubprogramEntry(node.element());
                                sc = getSourceCodeEntry(node);
                            }

                            private CallSiteTable.SourceCodeEntry getSourceCodeEntry(ProgramLocatable node) {
                                final ProgramLocatable inlinedAt = node.callSite();
                                return cst.intern(new CallSiteTable.SourceCodeEntry(cst.getSubprogramEntry(node.element()), node.lineNumber(), node.bytecodeIndex(), inlinedAt == null ? null : getSourceCodeEntry(inlinedAt)));
                            }

                            public void location(int locIndex, LocationType type, int size, int regNum, long data) {
                                switch (locIndex) {
                                    case 0 -> {
                                        // calling convention
                                        switch (type) {
                                            case Constant -> cconv = CallingConvention.values()[(int)data];
                                            default -> ctxt.error(Location.builder().setSourceFilePath(objectFile.toString()).build(), "Unexpected entry for calling convention");
                                        }
                                    }
                                    case 1 -> {
                                        // flags (ignore)
                                        switch (type) {
                                            case Constant -> {
                                            }
                                            default -> ctxt.error(Location.builder().setSourceFilePath(objectFile.toString()).build(), "Unexpected entry for flags");
                                        }
                                    }
                                    case 2 -> {
                                        // deopt locations (ignore)
                                        switch (type) {
                                            case Constant -> {
                                                if (data != 0) {
                                                    ctxt.error(Location.builder().setSourceFilePath(objectFile.toString()).build(), "Unexpected non-zero entry for deopt locations");
                                                }
                                            }
                                            default -> ctxt.error(Location.builder().setSourceFilePath(objectFile.toString()).build(), "Unexpected entry for deopt locations");
                                        }
                                    }
                                    default -> {
                                        // gather both base and derived pointers
                                        switch (type) {
                                            case Register -> valueInfos.add(RegisterValueInfo.forRegisterNumber(regNum));
                                            case Direct -> valueInfos.add(new RegisterRelativeValueInfo(RegisterValueInfo.forRegisterNumber(regNum), (int) (data / ctxt.getTypeSystem().getReferenceSize())));
                                            case Indirect -> valueInfos.add(new FrameOffsetValueInfo(RegisterValueInfo.forRegisterNumber(regNum), (int) (data / ctxt.getTypeSystem().getReferenceSize())));
                                            case Constant -> {
                                                if (data != 0) {
                                                    ctxt.error(Location.builder().setSourceFilePath(objectFile.toString()).build(), "Constant stack map record not supported");
                                                }
                                                // otherwise ignore (null values are expected)
                                            }
                                        }
                                    }
                                }
                            }

                            public void endRecord(long recIndex) {
                                final CallSiteTable.LiveValueInfo lvi = cst.intern(valueInfos);
                                valueInfos.clear();
                                fnCallSites.add(new CallSiteTable.CallSiteEntry(functionAddress, offset, sc, lvi));
                            }

                            public void endFunction(long fnIndex) {
                                fnCallSites.sort(Comparator.comparingLong(CallSiteTable.CallSiteEntry::offset));
                                callSites.addAll(fnCallSites);
                                fnCallSites.clear();
                            }

                            public void end() {
                                cst.registerEntries(typeDefinition, callSites);
                                callSites.clear();
                            }
                        });
                    }
                } catch (IOException e) {
                    ctxt.error(Location.builder().setSourceFilePath(String.valueOf(objFile)).build(), "Failed to read stack map information: %s", e);
                }
            }
        });
    }

    public static void execute(final CompilationContext ctxt) {
        new LLVMStackMapCollector(ctxt).collect();
    }
}
