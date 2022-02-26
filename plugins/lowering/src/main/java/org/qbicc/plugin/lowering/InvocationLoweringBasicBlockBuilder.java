package org.qbicc.plugin.lowering;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.CurrentThread;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.StaticMethodPointerHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.Section;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.plugin.dispatch.DispatchTables;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

/**
 *
 */
public class InvocationLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<ArrayList<Value>, ValueHandle> {
    private final CompilationContext ctxt;
    private final ExecutableElement originalElement;

    public InvocationLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        originalElement = delegate.getCurrentElement();
    }

    @Override
    public Value load(ValueHandle handle, ReadAccessMode accessMode) {
        if (handle instanceof CurrentThread ct) {
            return parameter(ct.getValueType(), "thr", 0);
        }
        return super.load(handle, accessMode);
    }

    @Override
    public ValueHandle currentThread() {
        if (originalElement instanceof FunctionElement fe) {
            Section section = ctxt.getImplicitSection(fe.getEnclosingType());
            ReferenceType type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Thread").load().getClassType().getReference();
            DataDeclaration decl = section.declareData(null, "_qbicc_bound_thread", type);
            decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
            final LiteralFactory lf = ctxt.getLiteralFactory();
            return pointerHandle(lf.literalOf(decl));
        }
        return super.currentThread();
    }

    public Value call(ValueHandle target, List<Value> arguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.call(target.accept(this, argList), argList);
    }

    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.callNoSideEffects(target.accept(this, argList), argList);
    }

    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.callNoReturn(target.accept(this, argList), argList);
    }

    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.invokeNoReturn(target.accept(this, argList), argList, catchLabel);
    }

    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.tailCall(target.accept(this, argList), argList);
    }

    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.tailInvoke(target.accept(this, argList), argList, catchLabel);
    }

    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        ArrayList<Value> argList = new ArrayList<>(arguments);
        return super.invoke(target.accept(this, argList), argList, catchLabel, resumeLabel);
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, ConstructorElementHandle node) {
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.load(fb.currentThread(), SingleUnshared), node.getInstance()));
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        node.getExecutable();
        FunctionDeclaration decl = ctxt.getImplicitSection(originalElement).declareFunction(function);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return pointerHandle(lf.literalOf(decl));
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, FunctionElementHandle node) {
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        node.getExecutable();
        FunctionDeclaration decl = ctxt.getImplicitSection(originalElement).declareFunction(function);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return pointerHandle(lf.literalOf(decl));
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, ExactMethodElementHandle node) {
        if (!node.getExecutable().hasMethodBodyFactory() && node.getExecutable().hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            // Convert native method that wasn't intercepted by an intrinsic to a runtime link error
            throw new BlockEarlyTermination(raiseLinkError(node.getExecutable()));
        }
        if (!ctxt.wasEnqueued(node.getExecutable())) {
            // No realized invocation targets are possible for this method!
            throw new BlockEarlyTermination(unreachable());
        }
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.load(fb.currentThread(), SingleUnshared), node.getInstance()));
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        FunctionDeclaration decl = ctxt.getImplicitSection(originalElement).declareFunction(function);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return pointerHandle(lf.literalOf(decl), lf.literalOf(0));
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, VirtualMethodElementHandle node) {
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
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            section.declareData(null, vtables.getName(), vtables.getType());
        }
        int index = dt.getVTableIndex(target);
        Value typeId = fb.load(fb.instanceFieldOf(fb.referenceHandle(node.getInstance()), CoreClasses.get(ctxt).getObjectTypeIdField()));
        Value vtable = fb.load(elementOf(globalVariable(dt.getVTablesGlobal()), typeId));
        Value ptr = fb.load(memberOf(pointerHandle(bitCast(vtable, info.getType().getPointer())), info.getType().getMember(index)));
        return pointerHandle(ptr);
    }

    // Current implementation strategy is "searched itables" in the terminology of [Alpern et al 2001].
    @Override
    public ValueHandle visit(ArrayList<Value> args, InterfaceMethodElementHandle node) {
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

        Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
        GlobalVariableElement rootITables = dt.getITablesGlobal();
        if (!rootITables.getEnclosingType().equals(originalElement.getEnclosingType())) {
            section.declareData(null, rootITables.getName(), rootITables.getType());
        }

        // Use the receiver's typeId to get the itable dictionary for its class
        Value typeId = fb.load(fb.instanceFieldOf(fb.referenceHandle(node.getInstance()), CoreClasses.get(ctxt).getObjectTypeIdField()));
        Value itableDict = fb.load(elementOf(globalVariable(rootITables), typeId));
        ValueHandle zeroElementHandle = fb.pointerHandle(itableDict);

        // Search loop to find the itableDictEntry with the typeId of the target interface.
        // If we hit the sentinel (typeid 0), then there was an IncompatibleClassChangeError
        BlockLabel failLabel = new BlockLabel();
        BlockLabel checkForICCE = new BlockLabel();
        BlockLabel exitMatched = new BlockLabel();
        BlockLabel loop = new BlockLabel();

        BasicBlock initial = goto_(loop);
        begin(loop);
        PhiValue phi = phi(ctxt.getTypeSystem().getSignedInteger32Type(), loop);
        IntegerLiteral zero = ctxt.getLiteralFactory().literalOf(0);
        phi.setValueForBlock(ctxt, getCurrentElement(), initial, zero);
        Value candidateId = fb.load(fb.memberOf(fb.elementOf(zeroElementHandle, phi), dt.getItableDictType().getMember("typeId")));
        if_(isEq(candidateId, ctxt.getLiteralFactory().literalOf(info.getInterface().getTypeId())), exitMatched, checkForICCE);
        try {
            begin(checkForICCE);
            BasicBlock body = if_(isEq(candidateId, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(info.getInterface().getType().getTypeType())), failLabel, loop);
            phi.setValueForBlock(ctxt, getCurrentElement(), body, fb.add(phi, ctxt.getLiteralFactory().literalOf(1)));

            begin(failLabel);
            MethodElement method = RuntimeMethodFinder.get(ctxt).getMethod("raiseIncompatibleClassChangeError");
            callNoReturn(staticMethod(method), List.of());
        } catch (BlockEarlyTermination ignored) {
            // ignore; continue to generate validEntry block
        }
        begin(exitMatched);
        Value itable = fb.bitCast(fb.load(fb.memberOf(fb.elementOf(zeroElementHandle, phi), dt.getItableDictType().getMember("itable"))), info.getType().getPointer());
        final Value ptr = fb.load(memberOf(fb.pointerHandle(itable), info.getType().getMember(dt.getITableIndex(target))));
        return pointerHandle(ptr);
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, StaticMethodElementHandle node) {
        if (!node.getExecutable().hasMethodBodyFactory() && node.getExecutable().hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            // Convert native method that wasn't intercepted by an intrinsic to a runtime link error
            throw new BlockEarlyTermination(raiseLinkError(node.getExecutable()));
        }
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert current thread only
        args.add(0, fb.load(fb.currentThread(), SingleUnshared));
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        FunctionDeclaration decl = ctxt.getImplicitSection(originalElement).declareFunction(function);
        final LiteralFactory lf = ctxt.getLiteralFactory();
        return pointerHandle(lf.literalOf(decl));
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, StaticMethodPointerHandle node) {
        Value pointer = node.getStaticMethodPointer();
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert current thread only
        ValueHandle currentThread = fb.currentThread();
        args.add(0, fb.load(currentThread, SingleUnshared));
        // pointer type should already have been converted by MemberPointerCopier
        return fb.pointerHandle(pointer);
    }

    @Override
    public ValueHandle visitUnknown(ArrayList<Value> args, ValueHandle node) {
        // no conversion needed
        return node;
    }

    private BasicBlock raiseLinkError(MethodElement target) {
        // Perform the transformation done by ObjectLiteralSerializingVisitor.visit(StringLiteral) because this BBB runs during LOWER
        VmString vString = ctxt.getVm().intern(target.getEnclosingType().getInternalName().replace("/", ".")+"."+target.getName());
        ProgramObjectLiteral literal = BuildtimeHeap.get(ctxt).serializeVmObject(vString);
        ctxt.getImplicitSection(originalElement).declareData(literal.getProgramObject());
        Literal arg = ctxt.getLiteralFactory().bitcastLiteral(literal, ctxt.getBootstrapClassContext().findDefinedType("java/lang/String").load().getType().getReference());

        MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseUnsatisfiedLinkError");
        return callNoReturn(staticMethod(helper), List.of(arg));
    }
}
