package org.qbicc.driver;

/**
 * The builder operation stage within a {@linkplain Phase build phase}.
 */
public enum BuilderStage {
    /**
     * This stage should be used for operations which transform or lower nodes.
     */
    TRANSFORM,
    /**
     * This stage should be used for operations which automatically correct inconsistencies.
     */
    CORRECT,
    /**
     * This stage should be used for optimization stages.
     */
    OPTIMIZE,
    /**
     * This stage should be used for final integrity checks and error reporting, and for compiling information to be
     * used in the next stage.
     */
    INTEGRITY,
    ;
}
