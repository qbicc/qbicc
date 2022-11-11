package org.qbicc.machine.llvm.op;

import java.util.Set;

import org.qbicc.machine.llvm.Attributable;
import org.qbicc.machine.llvm.CallingConvention;
import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface Call extends YieldingInstruction, Attributable, HasArguments {
    Call withFlags(Set<FastMathFlag> flags);

    Call tail();

    Call mustTail();

    Call noTail();

    Call cconv(CallingConvention cconv);

    Returns returns();

    Call addrSpace(int num);

    Call comment(String comment);

    Call meta(String name, LLValue data);

    Call attribute(LLValue attribute);

    HasArguments operandBundle(String bundleName);

    interface Returns {
        Returns attribute(LLValue attribute);
    }

}
