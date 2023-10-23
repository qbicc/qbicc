package org.qbicc.plugin.intrinsics.core;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.Os;
import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.arch.Platform;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * Intrinsics which answer build host and target queries.
 */
final class BuildIntrinsics {
    public static void register(CompilationContext ctxt) {
        Intrinsics intrinsics = Intrinsics.get(ctxt);
        registerTopLevelIntrinsics(intrinsics, ctxt);
        registerHostIntrinsics(intrinsics, ctxt);
        registerTargetIntrinsics(intrinsics, ctxt);
    }

    private static void registerTopLevelIntrinsics(final Intrinsics intrinsics, final CompilationContext ctxt) {
        final ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor buildDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        intrinsics.registerIntrinsic(Phase.ANALYZE, buildDesc, "isHost", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(false)
        );
        intrinsics.registerIntrinsic(Phase.ANALYZE, buildDesc, "isTarget", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(true)
        );
        intrinsics.registerIntrinsic(buildDesc, "isJvm", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(false)
        );
    }

    private static void registerHostIntrinsics(final Intrinsics intrinsics, final CompilationContext ctxt) {
        final ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor hostDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build$Host");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        final Os os = Platform.HOST_PLATFORM.os();

        intrinsics.registerIntrinsic(hostDesc, "isLinux", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.linux)
        );
        intrinsics.registerIntrinsic(hostDesc, "isWindows", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.win32)
        );
        intrinsics.registerIntrinsic(hostDesc, "isMacOs", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.macos)
        );

        final Cpu cpu = Platform.HOST_PLATFORM.cpu();

        intrinsics.registerIntrinsic(hostDesc, "isAmd64", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.x64)
        );
        intrinsics.registerIntrinsic(hostDesc, "isI386", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.x86)
        );
        intrinsics.registerIntrinsic(hostDesc, "isArm", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.arm)
        );
        intrinsics.registerIntrinsic(hostDesc, "isAarch64", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.aarch64)
        );
    }

    private static void registerTargetIntrinsics(final Intrinsics intrinsics, final CompilationContext ctxt) {
        final ClassContext classContext = ctxt.getBootstrapClassContext();

        ClassTypeDescriptor targetDesc = ClassTypeDescriptor.synthesize(classContext, "org/qbicc/runtime/Build$Target");

        MethodDescriptor emptyToBool = MethodDescriptor.synthesize(classContext, BaseTypeDescriptor.Z, List.of());

        intrinsics.registerIntrinsic(targetDesc, "isVirtual", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(true)
        );

        final Os os = ctxt.getPlatform().os();

        intrinsics.registerIntrinsic(targetDesc, "isUnix", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.linux || os == Os.macos)
        );
        intrinsics.registerIntrinsic(targetDesc, "isLinux", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.linux)
        );
        intrinsics.registerIntrinsic(targetDesc, "isWindows", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.win32)
        );
        intrinsics.registerIntrinsic(targetDesc, "isApple", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.macos)
        );
        intrinsics.registerIntrinsic(targetDesc, "isMacOs", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.macos)
        );
        intrinsics.registerIntrinsic(targetDesc, "isIOS", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(false)
        );
        intrinsics.registerIntrinsic(targetDesc, "isAix", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(false)
        );
        intrinsics.registerIntrinsic(targetDesc, "isPosix", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.linux || os == Os.macos)
        );
        intrinsics.registerIntrinsic(targetDesc, "isWasi", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(os == Os.wasi)
        );

        final Cpu cpu = ctxt.getPlatform().cpu();

        intrinsics.registerIntrinsic(targetDesc, "isAmd64", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.x64)
        );
        intrinsics.registerIntrinsic(targetDesc, "isI386", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.x86)
        );
        intrinsics.registerIntrinsic(targetDesc, "isArm", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.arm)
        );
        intrinsics.registerIntrinsic(targetDesc, "isAarch64", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.aarch64)
        );
        intrinsics.registerIntrinsic(targetDesc, "isWasm", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(cpu == Cpu.wasm32)
        );

        final ObjectType objectType = ctxt.getPlatform().objectType();

        intrinsics.registerIntrinsic(targetDesc, "isElf", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(objectType == ObjectType.elf)
        );
        intrinsics.registerIntrinsic(targetDesc, "isMachO", emptyToBool, (builder, targetPtr, arguments) ->
            builder.getLiteralFactory().literalOf(objectType == ObjectType.macho)
        );
    }
}
