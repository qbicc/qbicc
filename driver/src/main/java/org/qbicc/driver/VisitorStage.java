package org.qbicc.driver;

/**
 * The visitor operation stage within a {@linkplain Phase build phase}.
 */
public enum VisitorStage {
    /**
     * This stage should be used for operations which transform nodes.
     */
    TRANSFORM,
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
