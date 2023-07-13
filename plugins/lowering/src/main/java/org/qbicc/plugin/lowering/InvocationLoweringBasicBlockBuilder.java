package org.qbicc.plugin.lowering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Load;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.Node;
import org.qbicc.graph.Slot;
import org.qbicc.graph.ThreadBound;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.literal.ExecutableLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.dispatch.DispatchTables;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.StructType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.ParameterElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.generic.BaseTypeSignature;

/**
 *
 */
public class InvocationLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final ExecutableElement originalElement;
    private final StructType threadNativeType;
    private final StructType.Member currentThreadMember;
    private final DefinedTypeDefinition threadTypeDef;
    private boolean started;
    private BlockParameter thrParam;

    public InvocationLoweringBasicBlockBuilder(final FactoryContext fc, final BasicBlockBuilder delegate) {
        super(delegate);
        CompilationContext ctxt = getContext();
        this.ctxt = ctxt;
        originalElement = delegate.getCurrentElement();
        final ClassContext bcc = ctxt.getBootstrapClassContext();
        threadNativeType = (StructType) bcc.resolveTypeFromClassName("jdk/internal/thread", "ThreadNative$thread_native");
        currentThreadMember = threadNativeType.getMember("ref");
        threadTypeDef = bcc.findDefinedType("java/lang/Thread");
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        Node node = super.begin(blockLabel);
        if (! started) {
            started = true;
            final LocalVariableElement.Builder lveBuilder = LocalVariableElement.builder(".thr", BaseTypeDescriptor.V, 0);
            lveBuilder.setSignature(BaseTypeSignature.V);
            lveBuilder.setEnclosingType(originalElement.getEnclosingType());
            lveBuilder.setType(threadNativeType.getPointer());
            lveBuilder.setLine(node.getSourceLine());
            lveBuilder.setBci(node.getBytecodeIndex());
            lveBuilder.setTypeParameterContext(getCurrentElement().getTypeParameterContext());
            lveBuilder.setSourceFileName(originalElement.getSourceFileName());
            final LocalVariableElement lve = lveBuilder.build();
            if (originalElement instanceof FunctionElement fe) {
                ProgramModule programModule = ctxt.getOrAddProgramModule(fe.getEnclosingType());
                DataDeclaration decl = programModule.declareData(null, "_qbicc_bound_java_thread", threadNativeType.getPointer());
                decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
                final LiteralFactory lf = ctxt.getLiteralFactory();
                declareDebugAddress(lve, lf.literalOf(decl));
            } else {
                ParameterElement.Builder peBuilder = ParameterElement.builder(".thr", BaseTypeDescriptor.V, 0);
                peBuilder.setSignature(BaseTypeSignature.V);
                peBuilder.setEnclosingType(originalElement.getEnclosingType());
                peBuilder.setType(threadNativeType.getPointer());
                peBuilder.setTypeParameterContext(getCurrentElement().getTypeParameterContext());
                peBuilder.setSourceFileName(originalElement.getSourceFileName());
                lveBuilder.setReflectsParameter(peBuilder.build());
                thrParam = addParam(blockLabel, Slot.thread(), threadNativeType.getPointer(), false);
                setDebugValue(lve, thrParam);
            }
        }
        return node;
    }

    @Override
    public Value currentThread() {
        if (originalElement instanceof FunctionElement fe) {
            ProgramModule programModule = ctxt.getOrAddProgramModule(fe.getEnclosingType());
            DataDeclaration decl = programModule.declareData(null, "_qbicc_bound_java_thread", threadNativeType.getPointer());
            decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
            final LiteralFactory lf = ctxt.getLiteralFactory();
            return memberOf(load(lf.literalOf(decl)), currentThreadMember);
        } else {
            return memberOf(thrParam, currentThreadMember);
        }
    }

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        // optimize %thr->ref->threadNativePtr to just %thr
        if (pointer instanceof MemberOf ifo
            && ifo.getMember().getName().equals("threadNativePtr")
            && ifo.getStructurePointer() instanceof DecodeReference dr
            && dr.getInput() instanceof Load load
            && load.getPointer() instanceof MemberOf mo
            && mo.getMember().equals(currentThreadMember)
            && mo.getStructType().equals(threadNativeType)
            // do this check *last*
            && ifo.getStructType().equals(Layout.get(ctxt).getInstanceLayoutInfo(threadTypeDef).getStructType())
        ) {
            return mo.getStructurePointer();
        } else {
            return super.load(pointer, accessMode);
        }
    }

    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        ArrayList<Value> argList = copyArgList(arguments);
        return super.call(lower(targetPtr, receiver, argList), receiver, argList);
    }

    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        ArrayList<Value> argList = copyArgList(arguments);
        return super.callNoSideEffects(lower(targetPtr, receiver, argList), receiver, argList);
    }

    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        ArrayList<Value> argList = copyArgList(arguments);
        return super.callNoReturn(lower(targetPtr, receiver, argList), receiver, argList);
    }

    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        ArrayList<Value> argList = copyArgList(arguments);
        return super.invokeNoReturn(lower(targetPtr, receiver, argList), receiver, argList, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        ArrayList<Value> argList = copyArgList(arguments);
        return super.tailCall(lower(targetPtr, receiver, argList), receiver, argList);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        ArrayList<Value> argList = copyArgList(arguments);
        return super.invoke(lower(targetPtr, receiver, argList), receiver, argList, catchLabel, resumeLabel, targetArguments);
    }

    private static ArrayList<Value> copyArgList(final List<Value> arguments) {
        ArrayList<Value> list = new ArrayList<>(arguments.size() + 2);
        list.addAll(arguments);
        return list;
    }

    private Value lower(Value targetPtr, Value receiver, ArrayList<Value> args) {
        InvokableType invType = targetPtr.getPointeeType(InvokableType.class);
        if (! (invType instanceof FunctionType)) {
            // insert current thread native pointer
            Value threadPtr;
            if (targetPtr instanceof ThreadBound tb) {
                // the thread pointer comes from an explicit binding
                threadPtr = tb.getThreadPointer();
                targetPtr = tb.getTarget();
            } else if (originalElement instanceof FunctionElement fe) {
                ProgramModule programModule = ctxt.getOrAddProgramModule(fe.getEnclosingType());
                DataDeclaration decl = programModule.declareData(null, "_qbicc_bound_java_thread", threadNativeType.getPointer());
                decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
                final LiteralFactory lf = ctxt.getLiteralFactory();
                threadPtr = load(lf.literalOf(decl));
            } else {
                threadPtr = thrParam;
            }
            args.add(0, threadPtr);
            if (invType instanceof InstanceMethodType) {
                // also insert the receiver
                args.add(1, receiver);
            }
        }
        // now lower to function
        if (targetPtr instanceof ExecutableLiteral el) {
            ExecutableElement element = el.getExecutable();
            if (! element.hasMethodBodyFactory() && element.hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
                // Convert native method that wasn't intercepted by an intrinsic to a runtime link error
                throw new BlockEarlyTermination(raiseLinkError((MethodElement) element));
            }
            if (!ctxt.mayBeEnqueued(element)) {
                // No realized invocation targets are possible for this method!
                throw new BlockEarlyTermination(unreachable());
            }
            ctxt.enqueue(element);
            Function function = ctxt.getExactFunction(element);
            FunctionDeclaration decl = ctxt.getOrAddProgramModule(originalElement).declareFunction(function);
            final LiteralFactory lf = ctxt.getLiteralFactory();
            return lf.literalOf(decl);
        } else {
            // already lowered pointer; we may have to bitcast it
            FunctionType lowerInvType = ctxt.getFunctionTypeForInvokableType(invType);
            return invType == lowerInvType ? targetPtr : bitCast(targetPtr, lowerInvType.getPointer());
        }
    }

    @Override
    public Value lookupVirtualMethod(Value reference, InstanceMethodElement target) {
        BasicBlockBuilder fb = getFirstBuilder();
        LiteralFactory lf = getLiteralFactory();
        if (!ReachabilityInfo.get(ctxt).isDispatchableMethod(target)) {
            // No realized invocation targets are possible for this method!
            return lf.nullLiteralOfType(target.getType().getPointer());
        }
        DispatchTables dt = DispatchTables.get(ctxt);
        DispatchTables.VTableInfo info = dt.getVTableInfo(target.getEnclosingType().load());
        Assert.assertNotNull(info); // If the target method is invokable, there must be a VTableInfo for it.
        GlobalVariableElement vtables = dt.getVTablesGlobal();
        if (!vtables.getEnclosingType().equals(originalElement.getEnclosingType())) {
            ProgramModule programModule = ctxt.getOrAddProgramModule(originalElement.getEnclosingType());
            programModule.declareData(null, vtables.getName(), vtables.getType());
        }
        int index = dt.getVTableIndex(target);
        Value typeId = fb.load(instanceFieldOf(fb.decodeReference(reference), CoreClasses.get(ctxt).getObjectTypeIdField()));
        Value vtable = fb.load(elementOf(lf.literalOf(dt.getVTablesGlobal()), typeId));
        return fb.load(memberOf(bitCast(vtable, info.getType().getPointer()), info.getType().getMember(index)));
    }

    // Current implementation strategy is "searched itables" in the terminology of [Alpern et al 2001].
    @Override
    public Value lookupInterfaceMethod(Value reference, InstanceMethodElement target) {
        final BasicBlockBuilder fb = getFirstBuilder();
        LiteralFactory lf = getLiteralFactory();
        DispatchTables dt = DispatchTables.get(ctxt);
        DispatchTables.ITableInfo info = dt.getITableInfo(target.getEnclosingType().load());
        if (info == null) {
            // No realized invocation targets are possible for this method!
            return lf.nullLiteralOfType(target.getType().getPointer());
        }

        ProgramModule programModule = ctxt.getOrAddProgramModule(originalElement.getEnclosingType());
        GlobalVariableElement rootITables = dt.getITablesGlobal();
        if (!rootITables.getEnclosingType().equals(originalElement.getEnclosingType())) {
            programModule.declareData(null, rootITables.getName(), rootITables.getType());
        }

        // Use the receiver's typeId to get the itable dictionary for its class
        Value typeId = fb.load(instanceFieldOf(fb.decodeReference(reference), CoreClasses.get(ctxt).getObjectTypeIdField()));
        Value itableDict = fb.load(elementOf(lf.literalOf(rootITables), typeId));

        // Search loop to find the itableDictEntry with the typeId of the target interface.
        // If we hit the sentinel (typeid 0), then there was an IncompatibleClassChangeError
        BlockLabel failLabel = new BlockLabel();
        BlockLabel checkForICCE = new BlockLabel();
        BlockLabel exitMatched = new BlockLabel();
        BlockLabel loop = new BlockLabel();

        TypeSystem ts = ctxt.getTypeSystem();
        SignedIntegerType u32 = ts.getSignedInteger32Type();
        IntegerLiteral zero = lf.literalOf(u32, 0);
        goto_(loop, Slot.temp(0), zero);
        begin(loop);
        BlockParameter bp = addParam(loop, Slot.temp(0), u32);
        Value candidateId = fb.load(fb.memberOf(fb.elementOf(itableDict, bp), dt.getItableDictType().getMember("typeId")));
        if_(isEq(candidateId, lf.literalOf(info.getInterface().getTypeId())), exitMatched, checkForICCE, Map.of());
        try {
            begin(checkForICCE);
            TypeIdType typeType = info.getInterface().getObjectType().getTypeType();
            if_(isEq(candidateId, lf.zeroInitializerLiteralOfType(typeType)), failLabel, loop, Map.of(Slot.temp(0), fb.add(bp, lf.literalOf(u32, 1))));

            begin(failLabel);
            MethodElement method = RuntimeMethodFinder.get(ctxt).getMethod("raiseIncompatibleClassChangeError");
            callNoReturn(lf.literalOf(method), List.of());
        } catch (BlockEarlyTermination ignored) {
            // ignore; continue to generate validEntry block
        }
        begin(exitMatched);
        Value itable = fb.bitCast(fb.load(fb.memberOf(fb.elementOf(itableDict, bp), dt.getItableDictType().getMember("itable"))), info.getType().getPointer());
        return fb.load(memberOf(itable, info.getType().getMember(dt.getITableIndex(target))));
    }

    private BasicBlock raiseLinkError(MethodElement target) {
        // Perform the transformation done by ObjectLiteralSerializingVisitor.visit(StringLiteral) because this BBB runs during LOWER
        VmString vString = ctxt.getVm().intern(target.getEnclosingType().getInternalName().replace("/", ".")+"."+target.getName());
        BuildtimeHeap bth = BuildtimeHeap.get(ctxt);
        bth.serializeVmObject(vString, true);
        Literal arg = bth.referToSerializedVmObject(vString,
            ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load().getObjectType().getReference(),
            ctxt.getOrAddProgramModule(originalElement));

        MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseUnsatisfiedLinkError");
        return callNoReturn(getLiteralFactory().literalOf(helper), List.of(arg));
    }
}
