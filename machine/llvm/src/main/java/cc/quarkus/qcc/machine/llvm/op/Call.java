package cc.quarkus.qcc.machine.llvm.op;

import java.util.Set;

import cc.quarkus.qcc.machine.llvm.CallingConvention;
import cc.quarkus.qcc.machine.llvm.FastMathFlag;
import cc.quarkus.qcc.machine.llvm.Value;

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

    Call addrSpace(int num);

    Call comment(String comment);

    Call meta(String name, Value data);

    Argument arg(Value type, Value value);

    interface Argument {
        Argument arg(Value type, Value value);

        Argument signExt();

        Argument zeroExt();

        Argument inReg();
    }
}
