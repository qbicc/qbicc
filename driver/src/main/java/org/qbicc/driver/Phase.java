package org.qbicc.driver;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.PhaseAttachmentKey;

/**
 * The overall build phase.  Each phase contains multiple {@linkplain BuilderStage stages}, with the exception
 * of the {@link #GENERATE} phase in which nodes are visited but not built.
 */
public enum Phase {
    /**
     * The first stage, where classes are loaded and initialized.
     */
    ADD ("add"),
    /**
     * The second stage where closed-world analysis is done and all build-time code is eliminated.
     */
    ANALYZE ("analyze"),
    /**
     * The third stage where high level nodes such as type IDs, invocations, and field access are lowered to
     * backend-compatible representations.
     */
    LOWER ("lower"),
    /**
     * The final stage where all reachable elements and nodes are visited by the back end generator(s) to produce
     * a runnable image.
     */
    GENERATE ("generate"),
    ;
    private static final Logger log = Logger.getLogger("org.qbicc.driver.phase");

    private final String phase;

    Phase(String phase) {
        this.phase = phase;
    }

    public String toString() {
        return phase;
    }

    public static Phase getPhase(String phase) {
        if (phase.equalsIgnoreCase("add")) {
            return ADD;
        } else if (phase.equalsIgnoreCase("analyze")) {
            return ANALYZE;
        } else if (phase.equalsIgnoreCase("lower")) {
            return LOWER;
        } else {
            return GENERATE;
        }
    }

    private static final PhaseAttachmentKey<Phase> KEY = new PhaseAttachmentKey<>();

    public static Phase getPrevious(CompilationContext ctxt) {
        return ctxt.getPreviousPhaseAttachment(KEY);
    }

    public static Phase getCurrent(CompilationContext ctxt) {
        return ctxt.getAttachment(KEY);
    }

    void setCurrent(CompilationContext ctxt) {
        complete(ctxt);
        ctxt.putAttachment(KEY, this);
        MDC.put("phase", name());
        ctxt.runParallelTask(context -> MDC.put("phase", name()));
        log.info("Entering phase");
    }

    static void complete(CompilationContext ctxt) {
        if (ctxt.getPreviousPhaseAttachment(KEY) != null) {
            log.info("Phase complete");
        }
        MDC.remove("phase");
    }
}
