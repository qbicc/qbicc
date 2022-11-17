package org.qbicc.plugin.reflection;

import static org.qbicc.plugin.reflection.Reflection.erase;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * A basic block builder which resolves signature-polymorphic {@code VarHandle} invocations to their
 * canonical variants; specifically, converting the return type as needed.
 */
public final class VarHandleResolvingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public VarHandleResolvingBasicBlockBuilder(FactoryContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value lookupVirtualMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        if (owner instanceof ClassTypeDescriptor ctd && ctd.packageAndClassNameEquals("java/lang/invoke", "VarHandle")) {
            return super.lookupVirtualMethod(reference, owner, name, translate(name, descriptor));
        }
        return super.lookupVirtualMethod(reference, owner, name, descriptor);
    }

    @Override
    public Value resolveInstanceMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        if (owner instanceof ClassTypeDescriptor ctd && ctd.packageAndClassNameEquals("java/lang/invoke", "VarHandle")) {
            return super.resolveInstanceMethod(owner, name, translate(name, descriptor));
        }
        return super.resolveInstanceMethod(owner, name, descriptor);
    }

    private MethodDescriptor translate(String name, MethodDescriptor descriptor) {
        ClassContext classContext = ctxt.getBootstrapClassContext();
        TypeDescriptor retType = descriptor.getReturnType();
        MethodDescriptor erased = erase(classContext, descriptor);
        List<TypeDescriptor> erasedParamTypes = erased.getParameterTypes();
        int paramCnt = erasedParamTypes.size();
        TypeDescriptor fixedRetType;
        // transform all special methods
        switch (name) {
            // CAS always returns {@code boolean}
            case "compareAndSet":
            case "weakCompareAndSetPlain":
            case "weakCompareAndSet":
            case "weakCompareAndSetAcquire":
            case "weakCompareAndSetRelease":
                fixedRetType = BaseTypeDescriptor.Z;
                break;

            // set always returns {@code void}
            case "set":
            case "setVolatile":
            case "setRelease":
            case "setOpaque":
                fixedRetType = retType;
                break;

            // get always returns *whatever the type is*; we can't really infer it though!
            case "get":
            case "getVolatile":
            case "getAcquire":
            case "getOpaque":
                fixedRetType = retType;
                break;

            // compare-and-exchange and read-modify-write returns the erased type of its input
            case "compareAndExchange":
            case "compareAndExchangeAcquire":
            case "compareAndExchangeRelease":
            case "getAndSet":
            case "getAndSetAcquire":
            case "getAndSetRelease":
            case "getAndAdd":
            case "getAndAddAcquire":
            case "getAndAddRelease":
            case "getAndBitwiseOr":
            case "getAndBitwiseOrAcquire":
            case "getAndBitwiseOrRelease":
            case "getAndBitwiseAnd":
            case "getAndBitwiseAndAcquire":
            case "getAndBitwiseAndRelease":
            case "getAndBitwiseXor":
            case "getAndBitwiseXorAcquire":
            case "getAndBitwiseXorRelease":
                fixedRetType = erasedParamTypes.get(paramCnt - 1);
                break;

            default:
                return descriptor;
        }
        if (retType == BaseTypeDescriptor.V) {
            return MethodDescriptor.synthesize(classContext, fixedRetType, erasedParamTypes);
        } else {
            // use the original return type even if it's narrower (because we will need the cast from the generated method)
            return MethodDescriptor.synthesize(classContext, retType, erasedParamTypes);
        }
    }
}
