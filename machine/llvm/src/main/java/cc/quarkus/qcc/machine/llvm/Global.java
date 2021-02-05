package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.op.YieldingInstruction;

/**
 *
 */
public interface Global extends YieldingInstruction {
    Global meta(String name, LLValue data);

    Global comment(String comment);

    Global value(LLValue value);

    Global dllStorageClass(DllStorageClass dllStorageClass);

    Global alignment(int alignment);

    Global preemption(RuntimePreemption preemption);

    Global section(String section);

    Global linkage(Linkage linkage);

    Global visibility(Visibility visibility);

    Global threadLocal(ThreadLocalStorageModel model);
}
