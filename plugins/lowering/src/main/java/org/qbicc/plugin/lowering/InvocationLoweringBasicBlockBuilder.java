package org.qbicc.plugin.lowering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.CurrentThread;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.Node;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.Slot;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;
import org.qbicc.graph.PointerValueVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
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
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.MethodType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

/**
 *
 */
public class InvocationLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder implements PointerValueVisitor<ArrayList<Value>, PointerValue> {
    private final CompilationContext ctxt;
    private final ExecutableElement originalElement;
    private final ReferenceType threadType;
    private boolean started;
    private BlockParameter thrParam;

    public InvocationLoweringBasicBlockBuilder(final FactoryContext fc, final BasicBlockBuilder delegate) {
        super(delegate);
        CompilationContext ctxt = getContext();
        this.ctxt = ctxt;
        originalElement = delegate.getCurrentElement();
        threadType = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Thread").load().getClassType().getReference();
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        Node node = super.begin(blockLabel);
        if (! started) {
            started = true;
            thrParam = addParam(blockLabel, Slot.thread(), threadType, false);
        }
        return node;
    }

    @Override
    public Value load(PointerValue handle, ReadAccessMode accessMode) {
        if (handle instanceof CurrentThread) {
            return getCurrentThreadRef();
        }
        return super.load(handle, accessMode);
    }

    @Override
    public PointerValue currentThread() {
        if (originalElement instanceof FunctionElement fe) {
            ProgramModule programModule = ctxt.getOrAddProgramModule(fe.getEnclosingType());
            DataDeclaration decl = programModule.declareData(null, "_qbicc_bound_java_thread", threadType);
            decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
            final LiteralFactory lf = ctxt.getLiteralFactory();
            return pointerHandle(lf.literalOf(decl));
        }
        // todo: replace this with addressOf(%thread) node after https://github.com/qbicc/qbicc/pull/1433
        return super.currentThread();
    }

    @Override
    public Value addressOf(final PointerValue handle) {
        if (handle instanceof StaticMethodElementHandle mh) {
            if (!mh.getExecutable().hasMethodBodyFactory() && mh.getExecutable().hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
                // Convert native method that wasn't intercepted by an intrinsic to a runtime link error
                throw new BlockEarlyTermination(raiseLinkError(mh.getExecutable()));
            }
            ctxt.enqueue(mh.getExecutable());
            Function function = ctxt.getExactFunction(mh.getExecutable());
            FunctionDeclaration decl = ctxt.getOrAddProgramModule(originalElement).declareFunction(function);
            final LiteralFactory lf = ctxt.getLiteralFactory();
            return super.addressOf(pointerHandle(lf.literalOf(decl)));
        }
        return super.addressOf(handle);
    }


    public Value call(PointerValue target, List<Value> arguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.call(target.accept(this, argList), argList);
    }

    public Value callNoSideEffects(PointerValue target, List<Value> arguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.callNoSideEffects(target.accept(this, argList), argList);
    }

    public BasicBlock callNoReturn(PointerValue target, List<Value> arguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.callNoReturn(target.accept(this, argList), argList);
    }

    public BasicBlock invokeNoReturn(PointerValue target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.invokeNoReturn(target.accept(this, argList), argList, catchLabel, targetArguments);
    }

    public BasicBlock tailCall(PointerValue target, List<Value> arguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.tailCall(target.accept(this, argList), argList);
    }

    public BasicBlock tailInvoke(PointerValue target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.tailInvoke(target.accept(this, argList), argList, catchLabel, targetArguments);
    }

    public Value invoke(PointerValue target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.invoke(target.accept(this, argList), argList, catchLabel, resumeLabel, targetArguments);
    }

    @Override
    public PointerValue visit(ArrayList<Value> args, ConstructorElementHandle node) {
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.load(fb.currentThread(), SingleUnshared), node.getInstance()));
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        node.getExecutable();
        FunctionDeclaration decl = ctxt.getOrAddProgramModule(originalElement).declareFunction(function);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return pointerHandle(lf.literalOf(decl));
    }

    @Override
    public PointerValue visit(ArrayList<Value> args, FunctionElementHandle node) {
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        node.getExecutable();
        FunctionDeclaration decl = ctxt.getOrAddProgramModule(originalElement).declareFunction(function);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return pointerHandle(lf.literalOf(decl));
    }

    @Override
    public PointerValue visit(ArrayList<Value> args, ExactMethodElementHandle node) {
        if (!node.getExecutable().hasMethodBodyFactory() && node.getExecutable().hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            // Convert native method that wasn't intercepted by an intrinsic to a runtime link error
            throw new BlockEarlyTermination(raiseLinkError(node.getExecutable()));
        }
        if (!ctxt.mayBeEnqueued(node.getExecutable())) {
            // No realized invocation targets are possible for this method!
            throw new BlockEarlyTermination(unreachable());
        }
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.load(fb.currentThread(), SingleUnshared), node.getInstance()));
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        FunctionDeclaration decl = ctxt.getOrAddProgramModule(originalElement).declareFunction(function);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return pointerHandle(lf.literalOf(decl), lf.literalOf(0));
    }

    @Override
    public PointerValue visit(ArrayList<Value> args, VirtualMethodElementHandle node) {
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.load(fb.currentThread(), SingleUnshared), node.getInstance()));
        final MethodElement target = node.getExecutable();
        if (!ReachabilityInfo.get(ctxt).isDispatchableMethod(target)) {
            // No realized invocation targets are possible for this method!
            throw new BlockEarlyTermination(unreachable());
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
        Value typeId = fb.load(fb.instanceFieldOf(fb.referenceHandle(node.getInstance()), CoreClasses.get(ctxt).getObjectTypeIdField()));
        Value vtable = fb.load(elementOf(globalVariable(dt.getVTablesGlobal()), typeId));
        Value ptr = fb.load(memberOf(pointerHandle(bitCast(vtable, info.getType().getPointer())), info.getType().getMember(index)));
        return pointerHandle(ptr);
    }

    // Current implementation strategy is "searched itables" in the terminology of [Alpern et al 2001].
    @Override
    public PointerValue visit(ArrayList<Value> args, InterfaceMethodElementHandle node) {
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.load(fb.currentThread(), SingleUnshared), node.getInstance()));
        final MethodElement target = node.getExecutable();
        DispatchTables dt = DispatchTables.get(ctxt);
        DispatchTables.ITableInfo info = dt.getITableInfo(target.getEnclosingType().load());
        if (info == null) {
            // No realized invocation targets are possible for this method!
            MethodElement method = RuntimeMethodFinder.get(ctxt).getMethod("raiseIncompatibleClassChangeError");
            throw new BlockEarlyTermination(fb.callNoReturn(staticMethod(method), List.of()));
        }

        ProgramModule programModule = ctxt.getOrAddProgramModule(originalElement.getEnclosingType());
        GlobalVariableElement rootITables = dt.getITablesGlobal();
        if (!rootITables.getEnclosingType().equals(originalElement.getEnclosingType())) {
            programModule.declareData(null, rootITables.getName(), rootITables.getType());
        }

        // Use the receiver's typeId to get the itable dictionary for its class
        Value typeId = fb.load(fb.instanceFieldOf(fb.referenceHandle(node.getInstance()), CoreClasses.get(ctxt).getObjectTypeIdField()));
        Value itableDict = fb.load(elementOf(globalVariable(rootITables), typeId));
        PointerValue zeroElementHandle = fb.pointerHandle(itableDict);

        // Search loop to find the itableDictEntry with the typeId of the target interface.
        // If we hit the sentinel (typeid 0), then there was an IncompatibleClassChangeError
        BlockLabel failLabel = new BlockLabel();
        BlockLabel checkForICCE = new BlockLabel();
        BlockLabel exitMatched = new BlockLabel();
        BlockLabel loop = new BlockLabel();

        LiteralFactory lf = ctxt.getLiteralFactory();
        TypeSystem ts = ctxt.getTypeSystem();
        SignedIntegerType u32 = ts.getSignedInteger32Type();
        IntegerLiteral zero = lf.literalOf(u32, 0);
        goto_(loop, Slot.temp(0), zero);
        begin(loop);
        BlockParameter bp = addParam(loop, Slot.temp(0), u32);
        Value candidateId = fb.load(fb.memberOf(fb.elementOf(zeroElementHandle, bp), dt.getItableDictType().getMember("typeId")));
        if_(isEq(candidateId, lf.literalOf(info.getInterface().getTypeId())), exitMatched, checkForICCE, Map.of());
        try {
            begin(checkForICCE);
            TypeType typeType = info.getInterface().getObjectType().getTypeType();
            if_(isEq(candidateId, lf.zeroInitializerLiteralOfType(typeType)), failLabel, loop, Map.of(Slot.temp(0), fb.add(bp, lf.literalOf(u32, 1))));

            begin(failLabel);
            MethodElement method = RuntimeMethodFinder.get(ctxt).getMethod("raiseIncompatibleClassChangeError");
            callNoReturn(staticMethod(method), List.of());
        } catch (BlockEarlyTermination ignored) {
            // ignore; continue to generate validEntry block
        }
        begin(exitMatched);
        Value itable = fb.bitCast(fb.load(fb.memberOf(fb.elementOf(zeroElementHandle, bp), dt.getItableDictType().getMember("itable"))), info.getType().getPointer());
        final Value ptr = fb.load(memberOf(fb.pointerHandle(itable), info.getType().getMember(dt.getITableIndex(target))));
        return pointerHandle(ptr);
    }

    @Override
    public PointerValue visit(ArrayList<Value> args, StaticMethodElementHandle node) {
        if (!node.getExecutable().hasMethodBodyFactory() && node.getExecutable().hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            // Convert native method that wasn't intercepted by an intrinsic to a runtime link error
            throw new BlockEarlyTermination(raiseLinkError(node.getExecutable()));
        }
        // insert current thread only
        args.add(0, getCurrentThreadRef());
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        FunctionDeclaration decl = ctxt.getOrAddProgramModule(originalElement).declareFunction(function);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return pointerHandle(lf.literalOf(decl));
    }

    @Override
    public PointerValue visit(ArrayList<Value> args, PointerHandle node) {
        // potentially, a pointer to a method
        Value pointerValue = node.getBaseValue();
        ValueType valueType = node.getPointeeType();
        if (valueType instanceof MethodType mt) {
            // we have to bitcast it, and also transform the arguments accordingly
            args.add(0, getCurrentThreadRef());
            // NOTE: calls to pointers to instance methods MUST already have the receiver set in the arg list by this point
            // todo: this is inconsistent with how instance method handles work
            //   - option 1: add a receiver argument to all call nodes, let backend decide what reg to use
            //   - option 2: receiver is first arg to all instance calls, backend can rearrange based on callee type
            return pointerHandle(bitCast(pointerValue, ctxt.getFunctionTypeForInvokableType(mt).getPointer()));
        }
        return node;
    }

    private Value getCurrentThreadRef() {
        BasicBlockBuilder fb = getFirstBuilder();
        if (originalElement instanceof FunctionElement fe) {
            ProgramModule programModule = ctxt.getOrAddProgramModule(fe.getEnclosingType());
            DataDeclaration decl = programModule.declareData(null, "_qbicc_bound_java_thread", threadType);
            decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
            final LiteralFactory lf = ctxt.getLiteralFactory();
            // load the ref from the TL
            return fb.load(fb.pointerHandle(lf.literalOf(decl)), SingleUnshared);
        } else {
            // it is a literal
            return thrParam;
        }
    }

    @Override
    public PointerValue visitUnknown(ArrayList<Value> args, PointerValue node) {
        // no conversion needed
        return node;
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
        return callNoReturn(staticMethod(helper), List.of(arg));
    }
}
