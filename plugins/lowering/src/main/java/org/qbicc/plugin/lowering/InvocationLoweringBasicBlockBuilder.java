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
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Function;
import org.qbicc.object.Section;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.dispatch.DispatchTables;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.MethodElement;

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

    public Value currentThread() {
        ReferenceType type = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Thread").load().getClassType().getReference();
        if (originalElement instanceof FunctionElement) {
            Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
            DataDeclaration decl = section.declareData(null, "_qbicc_bound_thread", type);
            decl.setThreadLocalMode(ThreadLocalMode.GENERAL_DYNAMIC);
            Value ptrVal = load(pointerHandle(ctxt.getLiteralFactory().literalOf(decl)), MemoryAtomicityMode.NONE);
            return valueConvert(ptrVal, type);
        } else {
            return parameter(type, "thr", 0);
        }
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
        args.addAll(0, List.of(fb.currentThread(), node.getInstance()));
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        ctxt.declareForeignFunction(node.getExecutable(), function, originalElement);
        return functionOf(function);
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, FunctionElementHandle node) {
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        ctxt.declareForeignFunction(node.getExecutable(), function, originalElement);
        return functionOf(function);
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, ExactMethodElementHandle node) {
        if (!ReachabilityInfo.get(ctxt).isInvokableMethod(node.getExecutable())) {
            // No realized invocation targets are possible for this method!
            throw new BlockEarlyTermination(unreachable());
        }
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.currentThread(), node.getInstance()));
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        ctxt.declareForeignFunction(node.getExecutable(), function, originalElement);
        return functionOf(function);
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, VirtualMethodElementHandle node) {
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.currentThread(), node.getInstance()));
        final MethodElement target = node.getExecutable();
        if (!ReachabilityInfo.get(ctxt).isInvokableMethod(target)) {
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
        Value typeId = fb.load(fb.instanceFieldOf(fb.referenceHandle(node.getInstance()), CoreClasses.get(ctxt).getObjectTypeIdField()), MemoryAtomicityMode.UNORDERED);
        Value vtable = fb.load(elementOf(globalVariable(dt.getVTablesGlobal()), typeId), MemoryAtomicityMode.UNORDERED);
        Value ptr = fb.load(memberOf(pointerHandle(bitCast(vtable, info.getType().getPointer())), info.getType().getMember(index)), MemoryAtomicityMode.UNORDERED);
        return pointerHandle(ptr);
    }

    // Current implementation strategy is "searched itables" in the terminology of [Alpern et al 2001].
    @Override
    public ValueHandle visit(ArrayList<Value> args, InterfaceMethodElementHandle node) {
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert "this" and current thread
        args.addAll(0, List.of(fb.currentThread(), node.getInstance()));
        final MethodElement target = node.getExecutable();
        DispatchTables dt = DispatchTables.get(ctxt);
        DispatchTables.ITableInfo info = dt.getITableInfo(target.getEnclosingType().load());
        if (info == null) {
            // No realized invocation targets are possible for this method!
            MethodElement method = ctxt.getVMHelperMethod("raiseIncompatibleClassChangeError");
            throw new BlockEarlyTermination(fb.callNoReturn(staticMethod(method, method.getDescriptor(), method.getType()), List.of()));
        }

        Section section = ctxt.getImplicitSection(originalElement.getEnclosingType());
        GlobalVariableElement rootITables = dt.getITablesGlobal();
        if (!rootITables.getEnclosingType().equals(originalElement.getEnclosingType())) {
            section.declareData(null, rootITables.getName(), rootITables.getType());
        }

        // Use the receiver's typeId to get the itable dictionary for its class
        Value typeId = fb.load(fb.instanceFieldOf(fb.referenceHandle(node.getInstance()), CoreClasses.get(ctxt).getObjectTypeIdField()), MemoryAtomicityMode.UNORDERED);
        Value itableDict = fb.load(elementOf(globalVariable(rootITables), typeId), MemoryAtomicityMode.UNORDERED);
        ValueHandle zeroElementHandle = fb.elementOf(fb.pointerHandle(itableDict), ctxt.getLiteralFactory().literalOf(0));

        // Search loop to find the itableDictEntry with the typeId of the target interface.
        // If we hit the sentinel (typeid 0), then there was an IncompatibleClassChangeError
        BlockLabel failLabel = new BlockLabel();
        BlockLabel checkForICCE = new BlockLabel();
        BlockLabel exitMatched = new BlockLabel();
        BlockLabel loop = new BlockLabel();

        BasicBlock initial = goto_(loop);
        begin(loop);
        PhiValue phi = phi(ctxt.getTypeSystem().getSignedInteger32Type(), loop);
        phi.setValueForBlock(ctxt, getCurrentElement(), initial, ctxt.getLiteralFactory().literalOf(0));
        Value candidateId = fb.load(fb.memberOf(fb.elementOf(zeroElementHandle, phi), dt.getItableDictType().getMember("typeId")), MemoryAtomicityMode.UNORDERED);
        if_(isEq(candidateId, ctxt.getLiteralFactory().literalOf(info.getInterface().getTypeId())), exitMatched, checkForICCE);
        try {
            begin(checkForICCE);
            BasicBlock body = if_(isEq(candidateId, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(info.getInterface().getType().getTypeType())), failLabel, loop);
            phi.setValueForBlock(ctxt, getCurrentElement(), body, fb.add(phi, ctxt.getLiteralFactory().literalOf(1)));

            begin(failLabel);
            MethodElement method = ctxt.getVMHelperMethod("raiseIncompatibleClassChangeError");
            callNoReturn(staticMethod(method, method.getDescriptor(), method.getType()), List.of());
        } catch (BlockEarlyTermination ignored) {
            // ignore; continue to generate validEntry block
        }
        begin(exitMatched);
        Value itable = fb.bitCast(fb.load(fb.memberOf(fb.elementOf(zeroElementHandle, phi), dt.getItableDictType().getMember("itable")), MemoryAtomicityMode.UNORDERED), info.getType().getPointer());
        final Value ptr = fb.load(memberOf(fb.pointerHandle(itable), info.getType().getMember(dt.getITableIndex(target))), MemoryAtomicityMode.UNORDERED);
        return pointerHandle(ptr);
    }

    @Override
    public ValueHandle visit(ArrayList<Value> args, StaticMethodElementHandle node) {
        final BasicBlockBuilder fb = getFirstBuilder();
        // insert current thread only
        args.add(0, fb.currentThread());
        ctxt.enqueue(node.getExecutable());
        Function function = ctxt.getExactFunction(node.getExecutable());
        ctxt.declareForeignFunction(node.getExecutable(), function, originalElement);
        return functionOf(function);
    }

    @Override
    public ValueHandle visitUnknown(ArrayList<Value> args, ValueHandle node) {
        // no conversion needed
        return node;
    }
}
