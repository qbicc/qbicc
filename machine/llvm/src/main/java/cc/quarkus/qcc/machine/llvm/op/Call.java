package cc.quarkus.qcc.machine.llvm.op;

import java.util.Set;

import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.FastMathFlag;
import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface Call extends YieldingInstruction {
    Call withFlags(Set<FastMathFlag> flags);

    Call tail();

    Call mustTail();

    Call noTail();

    Call cconv(CallingConvention cconv);

    // todo ret attrs

    Call signExt();

    Call zeroExt();

    Call addrSpace(int num);

    Call comment(String comment);

    Call meta(String name, LLValue data);

    Argument arg(LLValue type, LLValue value);

    interface Argument {
        Argument arg(LLValue type, LLValue value);

        Argument signExt();

        Argument zeroExt();

        Argument inReg();
    }
}
