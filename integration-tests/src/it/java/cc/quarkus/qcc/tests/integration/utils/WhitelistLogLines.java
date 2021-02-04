package cc.quarkus.qcc.tests.integration.utils;

import java.util.regex.Pattern;

/**
 * Whitelists errors in log files.
 *
 * @author Michal Karm Babacek <karm@redhat.com>
 */
public enum WhitelistLogLines {

    // This is appended to all undermentioned listings
    ALL(new Pattern[]{ //empty for now
    }),

    NONE(new Pattern[]{}),

    HELLO_WORLD(new Pattern[]{
            // Nothing for this app...
    });

    public final Pattern[] errs;

    WhitelistLogLines(Pattern[] errs) {
        this.errs = errs;
    }
}
