package org.qbicc.plugin.reachability;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * A block builder stage which recursively enqueues all referenced executable elements.
 * We implement an RTA-style analysis to identify reachable virtual methods based on
 * the set of reachable call sites and instantiated types.
 */
public class ReachabilityBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, Void> {
    private final CompilationContext ctxt;
    private final ExecutableElement originalElement;
    private final ReachabilityAnalysis analysis;

    public ReachabilityBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        this.originalElement = delegate.getCurrentElement();
        this.analysis = ReachabilityInfo.get(ctxt).getAnalysis();
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        target.accept(this, null);
        return super.call(target, arguments);
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        target.accept(this, null);
        return super.callNoSideEffects(target, arguments);
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        target.accept(this, null);
        return super.callNoReturn(target, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        target.accept(this, null);
        return super.invokeNoReturn(target, arguments, catchLabel);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        target.accept(this, null);
        return super.tailCall(target, arguments);
    }

    @Override
    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        target.accept(this, null);
        return super.tailInvoke(target, arguments, catchLabel);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        target.accept(this, null);
        return super.invoke(target, arguments, catchLabel, resumeLabel);
    }

    @Override
    public Void visit(Void param, ConstructorElementHandle node) {
        ConstructorElement target = node.getExecutable();
        LoadedTypeDefinition ltd = target.getEnclosingType().load();
        analysis.processReachableConstructorInvoke(ltd, target, originalElement);
        return null;
    }

    @Override
    public Void visit(Void param, FunctionElementHandle node) {
        FunctionElement target = node.getExecutable();
        analysis.processReachableStaticInvoke(target, originalElement);
        return null;
    }

    @Override
    public Void visit(Void param, ExactMethodElementHandle node) {
        analysis.processReachableInstanceMethodInvoke(node.getExecutable(), originalElement);
        return null;
    }

    @Override
    public Void visit(Void param, VirtualMethodElementHandle node) {
        analysis.processReachableInstanceMethodInvoke(node.getExecutable(), originalElement);
        return null;
    }

    @Override
    public Void visit(Void param, InterfaceMethodElementHandle node) {
        analysis.processReachableInstanceMethodInvoke(node.getExecutable(), originalElement);
        return null;
    }

    @Override
    public Void visit(Void param, StaticMethodElementHandle node) {
        MethodElement target = node.getExecutable();
        analysis.processStaticElementInitialization(target.getEnclosingType().load(), target, originalElement);
        analysis.processReachableStaticInvoke(target, originalElement);
        return null;
    }

    public Value newArray(final ArrayObjectType arrayType, Value size) {
        if (arrayType instanceof ReferenceArrayObjectType) {
            // Force the array's leaf element type to be reachable (and thus assigned a typeId).
            analysis.processArrayElementType(((ReferenceArrayObjectType)arrayType).getLeafElementType());
        }
        return super.newArray(arrayType, size);
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        if (arrayType instanceof ReferenceArrayObjectType) {
            // Force the array's leaf element type to be reachable (and thus assigned a typeId).
            analysis.processArrayElementType(((ReferenceArrayObjectType)arrayType).getLeafElementType());
        }
        return super.multiNewArray(arrayType, dimensions);
    }

    // TODO: only initialize the enclosing type if the static field is actually used for something
    @Override
    public ValueHandle staticField(FieldElement field) {
        analysis.processStaticElementInitialization(field.getEnclosingType().load(), field, originalElement);
        return super.staticField(field);
    }

    @Override
    public Value classOf(Value typeId, Value dimensions) {
        MethodElement methodElement = ctxt.getVMHelperMethod("classof_from_typeid");
        ctxt.enqueue(methodElement);
        if (typeId instanceof TypeLiteral typeLiteral && typeLiteral.getValue() instanceof ClassObjectType cot) {
            analysis.processClassInitialization(cot.getDefinition().load());
        }
        return super.classOf(typeId, dimensions);
    }
}
