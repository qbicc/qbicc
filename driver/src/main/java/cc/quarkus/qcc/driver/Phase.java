package cc.quarkus.qcc.driver;

/**
 * The overall build phase.  Each phase contains multiple {@linkplain BuilderStage stages}, with the exception
 * of the {@link #GENERATE} phase in which nodes are visited but not built.
 */
public enum Phase {
    /**
     * The first stage, where classes are loaded and initialized.
     */
    ADD,
    /**
     * The second stage where closed-world analysis is done and all build-time code is eliminated.
     */
    ANALYZE,
    /**
     * The third stage where high level nodes such as type IDs, invocations, and field access are lowered to
     * backend-compatible representations.
     */
    LOWER,
    /**
     * The final stage where all reachable elements and nodes are visited by the back end generator(s) to produce
     * a runnable image.
     */
    GENERATE,
    ;
}
