package cc.quarkus.qcc.machine.llvm;

import static java.util.Collections.*;
import static java.util.EnumSet.*;

import java.util.Set;

/**
 *
 */
public enum FastMathFlag {
    nnan,
    ninf,
    nsz,
    arcp,
    contract,
    afn,
    reassoc,
    ;
    public static final Set<FastMathFlag> fast = unmodifiableSet(allOf(FastMathFlag.class));
    public static final Set<FastMathFlag> none = emptySet();
}
