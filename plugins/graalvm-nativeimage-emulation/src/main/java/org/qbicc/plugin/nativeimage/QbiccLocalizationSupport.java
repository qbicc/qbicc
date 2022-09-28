package org.qbicc.plugin.nativeimage;

import com.oracle.svm.core.jdk.localization.LocalizationSupport;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;

public class QbiccLocalizationSupport extends LocalizationSupport {
    public QbiccLocalizationSupport(Locale defaultLocale, Set<Locale> locales, Charset defaultCharset) {
        super(defaultLocale, locales, defaultCharset);
    }
}
