package cc.quarkus.qcc.driver;

/**
 * The overall build phase.  Each phase contains multiple {@linkplain BuilderStage stages}.
 */
public enum Phase {
    /**
     * The first stage, where classes are loaded and initialized.
     */
    ADD,
    /**
     * The second stage, where the execution tracer emits nodes to be included in the final image, and these nodes
     * are then visited by the back end generator(s).
     */
    GENERATE,
    ;
}
