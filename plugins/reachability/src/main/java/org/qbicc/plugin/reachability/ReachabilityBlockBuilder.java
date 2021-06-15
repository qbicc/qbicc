package org.qbicc.plugin.reachability;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ClassOf;
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
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
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
    private final RTAInfo info;

    public ReachabilityBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        this.originalElement = delegate.getCurrentElement();
        this.info = RTAInfo.get(ctxt);
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        target.accept(this, null);

        /* Load service interfaces and implementations. Use META-INF/services files (TODO and eventually module-info)
         * to identify possible service implementations.
         *
         * ServiceLoader methods to look for are:
         * - load(Class<S> service)
         * - load(Class<S> service, ClassLoader loader)
         * - load(ModuleLayer layer, Class<S> service)
         * - loadInstalled(Class<S> service)
         *
         * TODO care about other parameters eventually... classloaders and modulelayer
         */
        if (target instanceof StaticMethodElementHandle) {
            StaticMethodElementHandle methodTarget = (StaticMethodElementHandle)target;
            MethodElement e = methodTarget.getExecutable();
            if (e.getSourceFileName().equals("ServiceLoader.java") && (e.getName().equals("load") || e.getName().equals("loadInstalled"))) {
                Value serviceInterfaceArg = arguments.get(0);
                if (arguments.size() == 2 && !(serviceInterfaceArg instanceof ClassOf)) {
                    /* For load(ModuleLayer layer, Class<S> service) service interface is the second argument */
                    serviceInterfaceArg = arguments.get(1);
                }
                if (serviceInterfaceArg instanceof ClassOf) {
                    Value input = ((ClassOf) serviceInterfaceArg).getInput();
                    if (input instanceof TypeLiteral) {
                        ValueType type = ((TypeLiteral) input).getValue();
                        if (type instanceof InterfaceObjectType) {
                            DefinedTypeDefinition serviceInterfaceDefinition = ((InterfaceObjectType) type).getDefinition();
                            LoadedTypeDefinition serviceInterface = serviceInterfaceDefinition.load();
                            info.processServiceInterface(serviceInterface, serviceInterfaceDefinition.getInternalName());
                        }
                    }
                }
            }

        }

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
        info.processClassInitialization(ltd);
        info.processInstantiatedClass(ltd, true, originalElement);
        ctxt.enqueue(target);
        return null;
    }

    @Override
    public Void visit(Void param, FunctionElementHandle node) {
        FunctionElement target = node.getExecutable();
        ctxt.enqueue(target);
        return null;
    }

    @Override
    public Void visit(Void param, ExactMethodElementHandle node) {
        info.processReachableInstanceMethodInvoke(node.getExecutable(), originalElement);
        return null;
    }

    @Override
    public Void visit(Void param, VirtualMethodElementHandle node) {
        info.processReachableInstanceMethodInvoke(node.getExecutable(), originalElement);
        return null;
    }

    @Override
    public Void visit(Void param, InterfaceMethodElementHandle node) {
        info.processReachableInstanceMethodInvoke(node.getExecutable(), originalElement);
        return null;
    }

    @Override
    public Void visit(Void param, StaticMethodElementHandle node) {
        MethodElement target = node.getExecutable();
        info.processStaticElementInitialization(target.getEnclosingType().load());
        ctxt.enqueue(target);
        return null;
    }

    public Value newArray(final ArrayObjectType arrayType, Value size) {
        if (arrayType instanceof ReferenceArrayObjectType) {
            // Force the array's leaf element type to be reachable (and thus assigned a typeId).
            info.processArrayElementType(((ReferenceArrayObjectType)arrayType).getLeafElementType());
        }
        info.processInstantiatedClass(Layout.get(ctxt).getArrayContentField(arrayType).getEnclosingType().load(), true, originalElement);
        return super.newArray(arrayType, size);
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        if (arrayType instanceof ReferenceArrayObjectType) {
            // Force the array's leaf element type to be reachable (and thus assigned a typeId).
            info.processArrayElementType(((ReferenceArrayObjectType)arrayType).getLeafElementType());
        }
        return super.multiNewArray(arrayType, dimensions);
    }

    // TODO: only enqueue the enclosing type if the static field is used for something
    @Override
    public ValueHandle staticField(FieldElement field) {
        info.processStaticElementInitialization(field.getEnclosingType().load());
        return super.staticField(field);
    }

    @Override
    public Value classOf(Value typeId) {
        MethodElement methodElement = ctxt.getVMHelperMethod("classof_from_typeid");
        ctxt.enqueue(methodElement);
        return super.classOf(typeId);
    }
}
