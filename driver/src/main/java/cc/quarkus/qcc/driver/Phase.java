package cc.quarkus.qcc.driver;

/**
 * The overall build phase.  Each phase contains multiple {@linkplain Stage stages}.
 */
public enum Phase {
    /**
     * The first stage, where classes are loaded and initialized.
     */
    ADDITIVE,
    /**
     * The second stage, where the execution tracer emits code to be included in the final image.
     */
    ANALYTIC,
    ;
}
