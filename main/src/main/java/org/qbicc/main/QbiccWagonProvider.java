package org.qbicc.main;

import java.lang.reflect.Field;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.eclipse.aether.transport.wagon.WagonProvider;

/**
 *
 */
final class QbiccWagonProvider implements WagonProvider {
    QbiccWagonProvider() {
    }

    public Wagon lookup(final String roleHint) {
        return switch (roleHint) {
            case "http" -> setAuthenticator(new LightweightHttpWagon());
            case "https" -> setAuthenticator(new LightweightHttpsWagon());
            case "file" -> new FileWagon();
            default -> throw new IllegalArgumentException();
        };
    }

    public void release(final Wagon wagon) {
    }

    private <W extends LightweightHttpWagon> W setAuthenticator(final W wagon) {
        final Field authenticator;
        try {
            // http://dev.eclipse.org/mhonarc/lists/aether-users/msg00113.html
            authenticator = LightweightHttpWagon.class.getDeclaredField("authenticator");
            authenticator.setAccessible(true);
            authenticator.set(wagon, new LightweightHttpWagonAuthenticator());
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        wagon.setPreemptiveAuthentication(true);
        return wagon;
    }
}
