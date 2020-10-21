package cc.quarkus.qcc.driver;

/**
 * The operation stage within a {@linkplain Phase build phase}.
 */
public enum Stage {
    /**
     * This stage should be used for operations which transform nodes.
     */
    TRANSFORM,
    /**
     * This stage should be used for operations which automatically correct inconsistencies.
     */
    CORRECT,
    /**
     * This stage should be used for lowering operations, including transforming field and method accesses to memory
     * accesses (only during the analytic stage).
     */
    LOWERING,
    /**
     * This stage should be used for optimization stages.
     */
    OPTIMIZE,
    /**
     * This stage should be used for final integrity checks and error reporting.  It can also be used in the additive
     * stage for gathering information for use in the analytic stage.
     */
    INTEGRITY,
    ;
}
