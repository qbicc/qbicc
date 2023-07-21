package org.qbicc.plugin.gc.common;

import static org.qbicc.graph.CmpAndSwap.Strength.STRONG;
import static org.qbicc.graph.atomic.AccessModes.SingleOpaque;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.logging.Logger;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ProgramObject;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.HeaderBits;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.StructType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
public abstract class AbstractGc {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.gc");
    private static final AttachmentKey<AbstractGc> GC_KEY = new AttachmentKey<>();

    private static final HeaderBits.Key MOVED_BIT = new HeaderBits.Key(1);

    private final CompilationContext ctxt;
    private final String name;
    private final MethodElement allocateMethod;
    private final MethodElement copyMethod;
    private final MethodElement zeroMethod;
    private final ClassObjectType stackObjectType;

    protected AbstractGc(CompilationContext ctxt, String name) {
        this.ctxt = ctxt;
        this.name = name;
        ClassContext classContext = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition defined = classContext.findDefinedType("jdk/internal/gc/Gc");
        if (defined == null) {
            throw runtimeMissing();
        }
        LoadedTypeDefinition loaded = defined.load();
        int index = loaded.findMethodIndex(e -> e.getName().equals("allocate"));
        if (index == -1) {
            throw methodMissing();
        }
        allocateMethod = loaded.getMethod(index);
        index = loaded.findMethodIndex(e -> e.getName().equals("copy"));
        if (index == -1) {
            throw methodMissing();
        }
        copyMethod = loaded.getMethod(index);
        index = loaded.findMethodIndex(e -> e.getName().equals("clear"));
        if (index == -1) {
            throw methodMissing();
        }
        zeroMethod = loaded.getMethod(index);
        defined = classContext.findDefinedType("org/qbicc/runtime/StackObject");
        if (defined == null) {
            throw runtimeMissing();
        }
        loaded = defined.load();
        stackObjectType = loaded.getClassType();
    }

    private static IllegalStateException methodMissing() {
        return new IllegalStateException("Required method or type is missing from jdk.internal.gc.Gc");
    }

    private static IllegalStateException runtimeMissing() {
        return new IllegalStateException("The jdk.internal.gc.Gc class is not present in the bootstrap class path");
    }

    public CompilationContext getCompilationContext() {
        return ctxt;
    }

    public static AbstractGc install(CompilationContext ctxt, String name) {
        AbstractGc gc;
        ServiceLoader<GcFactory> gcLoader = ServiceLoader.load(GcFactory.class);
        Iterator<GcFactory> it = gcLoader.iterator();
        for (;;) try {
            if (! it.hasNext()) {
                throw new IllegalStateException("No GC implementation named \"" + name + "\" found");
            }
            GcFactory next = it.next();
            if (next != null) {
                gc = next.createGc(ctxt, name);
                if (gc != null) {
                    break;
                }
            }
        } catch (ServiceConfigurationError e) {
            log.warnf(e, "Failed to load a service for GC when looking for GC named \"%s\"", name);
        }
        AbstractGc appearing = ctxt.putAttachmentIfAbsent(GC_KEY, gc);
        if (appearing != null) {
            throw new IllegalStateException("GC installed twice");
        }
        gc.installIntrinsics();
        return gc;
    }

    public static AbstractGc get(final CompilationContext ctxt) {
        AbstractGc gc = ctxt.getAttachment(GC_KEY);
        if (gc == null) {
            throw new IllegalStateException("GC not installed");
        }
        return gc;
    }

    public static void reserveMovedBit(CompilationContext ctxt) {
        HeaderBits.get(ctxt).getHeaderBits(MOVED_BIT);
    }

    public String getName() {
        return name;
    }

    private void installIntrinsics() {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        // install the intrinsics for the GC base class
        LiteralFactory lf = classContext.getLiteralFactory();
        TypeSystem ts = classContext.getTypeSystem();

        ClassTypeDescriptor gcDesc = ClassTypeDescriptor.synthesize(classContext, "jdk/internal/gc/Gc");
        ClassTypeDescriptor refDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$reference");
        ClassTypeDescriptor ptrDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/CNative$ptr");

        MethodDescriptor refToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of(refDesc));
        MethodDescriptor emptyToPtr = MethodDescriptor.synthesize(classContext, ptrDesc, List.of());
        MethodDescriptor emptyToInt = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.I, List.of());
        MethodDescriptor emptyToLong = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.J, List.of());

        final HeaderBits headerBits = HeaderBits.get(ctxt);
        final int movedIdx = headerBits.getHeaderBits(MOVED_BIT);
        final IntegerLiteral movedBitLiteral = lf.literalOf(headerBits.getHeaderType(), 1L << movedIdx);
        final CoreClasses cc = CoreClasses.get(ctxt);
        final InstanceFieldElement headerField = cc.getObjectHeaderField();
        intrinsics.registerIntrinsic(gcDesc, "setHeaderMovedBit", refToBool, (builder, targetPtr, arguments) -> {
            BlockLabel loop = new BlockLabel();
            BlockLabel resume = new BlockLabel();
            BlockLabel cas = new BlockLabel();
            final Value ptr = builder.decodeReference(arguments.get(0));
            final Value hfPtr = builder.instanceFieldOf(ptr, headerField);
            // enter the loop with a load
            builder.goto_(loop, Map.of(Slot.temp(0), builder.load(hfPtr, SingleOpaque)));
            // part 1: check to see if the moved bit is set
            final UnsignedIntegerType headerType = headerBits.getHeaderType();
            builder.begin(loop, sb -> {
                final BlockParameter oldVal = sb.addParam(loop, Slot.temp(0), headerType);
                final Value and = sb.and(oldVal, movedBitLiteral);
                final Value isUnmarked = sb.isEq(and, lf.literalOf(headerType, 0));
                sb.if_(isUnmarked, cas, resume, Map.of(Slot.temp(0), oldVal, Slot.temp(1), lf.literalOf(false)));
            });
            // part 2: the moved bit is clear; try to atomically set it, or retry on fail
            builder.begin(cas, sb -> {
                final StructType casResultType = CmpAndSwap.getResultType(ctxt, headerType);
                final BlockParameter oldVal = sb.addParam(cas, Slot.temp(0), headerType);
                Value or = sb.or(oldVal, movedBitLiteral);
                final Value casResult = sb.cmpAndSwap(hfPtr, oldVal, or, SingleOpaque, SingleOpaque, STRONG);
                final Value witness = sb.extractMember(casResult, casResultType.getMember(0));
                final Value success = sb.extractMember(casResult, casResultType.getMember(1));
                // on success, go to resume with temp 1 = true; on failure, go to loop with temp 0 (oldVal) = witness
                sb.if_(success, resume, loop, Map.of(Slot.temp(0), witness, Slot.temp(1), lf.literalOf(true)));
            });
            // result is in temp 1
            builder.begin(resume);
            return builder.addParam(resume, Slot.temp(1), ts.getBooleanType());
        });
        intrinsics.registerIntrinsic(gcDesc, "headerMovedBit", (builder, targetPtr, arguments) -> movedBitLiteral);

        final int saIdx = headerBits.getHeaderBits(CoreClasses.STACK_ALLOCATED_BIT);
        final IntegerLiteral stackAllocatedBitLiteral = lf.literalOf(headerBits.getHeaderType(), 1L << saIdx);
        intrinsics.registerIntrinsic(gcDesc, "headerStackAllocatedBit", (builder, targetPtr, arguments) -> stackAllocatedBitLiteral);

        ClassTypeDescriptor clsDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        ClassTypeDescriptor objDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Object");
        ClassTypeDescriptor ciDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/main/CompilerIntrinsics");

        MethodDescriptor copyDesc = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.V, List.of(clsDesc, objDesc, objDesc));

        StaticIntrinsic copy = (builder, target, arguments) -> {
            CoreClasses coreClasses = CoreClasses.get(getCompilationContext());
            Value cls = arguments.get(0);
            Value src = arguments.get(1);
            Value dst = arguments.get(2);
            Value size32 = builder.load(builder.instanceFieldOf(builder.decodeReference(cls), coreClasses.getClassInstanceSizeField()));
            Value size = builder.extend(size32, ctxt.getTypeSystem().getSignedInteger64Type());

            // TODO: This is a kludge:
            //  1. We are overwriting the object header fields initialized by new when doing the copy
            //     (to make sure we copy any instance fields that have been assigned to use the padding bytes in the basic object header).
            MethodElement method = AbstractGc.get(ctxt).getCopyMethod();
            return builder.call(builder.getLiteralFactory().literalOf(method), List.of(dst, src, size));
        };
        intrinsics.registerIntrinsic(Phase.LOWER, ciDesc, "copyInstanceFields", copyDesc, copy);

        intrinsics.registerIntrinsic(Phase.LOWER, gcDesc, "getClassRegionStart", emptyToPtr, (builder, targetPtr, arguments) -> {
            BuildtimeHeap buildtimeHeap = BuildtimeHeap.get(ctxt);
            ProgramObject rootArray = buildtimeHeap.getAndRegisterGlobalClassArray(builder.getRootElement());
            return builder.getLiteralFactory().literalOf(rootArray);
        });

        intrinsics.registerIntrinsic(Phase.LOWER, gcDesc, "getClassRegionSize", emptyToLong, (builder, targetPtr, arguments) -> {
            BuildtimeHeap buildtimeHeap = BuildtimeHeap.get(ctxt);
            ProgramObject rootArray = buildtimeHeap.getAndRegisterGlobalClassArray(builder.getRootElement());
            return builder.getLiteralFactory().literalOf(rootArray.getValueType(ArrayType.class).getSize());
        });

        intrinsics.registerIntrinsic(Phase.LOWER, gcDesc, "getInitialHeapRegionStart", emptyToPtr, (builder, targetPtr, arguments) -> {
            ProgramModule pm = ctxt.getOrAddProgramModule(builder.getRootElement());
            BuildtimeHeap bh = BuildtimeHeap.get(ctxt);
            DataDeclaration start = pm.declareData(bh.getHeapStart());
            return ctxt.getLiteralFactory().literalOf(start);
        });
        intrinsics.registerIntrinsic(Phase.LOWER, gcDesc, "getInitialHeapRegionSize", emptyToLong, (builder, targetPtr, arguments) -> {
            ProgramModule pm = ctxt.getOrAddProgramModule(builder.getRootElement());
            BuildtimeHeap bh = BuildtimeHeap.get(ctxt);
            DataDeclaration start = pm.declareData(bh.getHeapStart());
            DataDeclaration end = pm.declareData(bh.getHeapEnd());
            UnsignedIntegerType u8 = ctxt.getTypeSystem().getUnsignedInteger8Type();
            PointerType u8p = u8.getPointer();
            return builder.pointerDifference(lf.bitcastLiteral(lf.literalOf(end), u8p), lf.bitcastLiteral(lf.literalOf(start), u8p));
        });

        intrinsics.registerIntrinsic(Phase.LOWER, gcDesc, "getInitialHeapStringsRegionStart", emptyToPtr, (builder, targetPtr, arguments) -> {
            ProgramModule pm = ctxt.getOrAddProgramModule(builder.getRootElement());
            BuildtimeHeap bh = BuildtimeHeap.get(ctxt);
            DataDeclaration start = pm.declareData(bh.getStringsStart());
            return ctxt.getLiteralFactory().literalOf(start);
        });
        intrinsics.registerIntrinsic(Phase.LOWER, gcDesc, "getInitialHeapStringsRegionSize", emptyToLong, (builder, targetPtr, arguments) -> {
            ProgramModule pm = ctxt.getOrAddProgramModule(builder.getRootElement());
            BuildtimeHeap bh = BuildtimeHeap.get(ctxt);
            DataDeclaration start = pm.declareData(bh.getStringsStart());
            DataDeclaration end = pm.declareData(bh.getStringsEnd());
            UnsignedIntegerType u8 = ctxt.getTypeSystem().getUnsignedInteger8Type();
            PointerType u8p = u8.getPointer();
            return builder.pointerDifference(lf.bitcastLiteral(lf.literalOf(end), u8p), lf.bitcastLiteral(lf.literalOf(start), u8p));
        });

        intrinsics.registerIntrinsic(Phase.LOWER, gcDesc, "getReferenceTypedVariablesStart", emptyToPtr, (builder, targetPtr, arguments) -> {
            ProgramModule pm = ctxt.getOrAddProgramModule(builder.getRootElement());
            BuildtimeHeap bh = BuildtimeHeap.get(ctxt);
            DataDeclaration start = pm.declareData(bh.getRefsStart());
            return ctxt.getLiteralFactory().literalOf(start);
        });
        intrinsics.registerIntrinsic(Phase.LOWER, gcDesc, "getReferenceTypedVariablesCount", emptyToInt, (builder, targetPtr, arguments) -> {
            ProgramModule pm = ctxt.getOrAddProgramModule(builder.getRootElement());
            BuildtimeHeap bh = BuildtimeHeap.get(ctxt);
            DataDeclaration start = pm.declareData(bh.getRefsStart());
            DataDeclaration end = pm.declareData(bh.getRefsEnd());
            PointerType refPointerType = start.getSymbolType();
            SignedIntegerType s32 = ctxt.getTypeSystem().getSignedInteger32Type();
            Value diff = builder.pointerDifference(lf.bitcastLiteral(lf.literalOf(end), refPointerType), lf.literalOf(start));
            if (diff.getType(SignedIntegerType.class) != s32) {
                diff = builder.truncate(diff, s32);
            }
            return diff;
        });

    }

    public MethodElement getAllocateMethod() {
        return allocateMethod;
    }

    public MethodElement getCopyMethod() {
        return copyMethod;
    }

    public MethodElement getZeroMethod() {
        return zeroMethod;
    }

    public ClassObjectType getStackObjectType() {
        return stackObjectType;
    }
}
