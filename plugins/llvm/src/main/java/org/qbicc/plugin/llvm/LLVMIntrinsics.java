package org.qbicc.plugin.llvm;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.intrinsics.StaticIntrinsic;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

public final class LLVMIntrinsics {
    public static void register(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor buildTargetDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build$Target");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        StaticIntrinsic isLlvm = (builder, target, arguments) -> ctxt.getLiteralFactory().literalOf(true);
        intrinsics.registerIntrinsic(buildTargetDesc, "isLlvm", emptyToBool, isLlvm);
    }
}
