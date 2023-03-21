package org.qbicc.test.quarkus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.spi.HttpResponse;

/**
 * A simple service for hashing text.
 */
@Path("/hash")
public class HashService {
    @Context
    HttpResponse response;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @QueryParam("q")
    public String hash(String hashString) {
        if (hashString == null || hashString.isEmpty()) {
            return "Send in the string to be hashed using the \"q\" query parameter";
        }
        final StringBuilder b = new StringBuilder(200).append("The hash of \"").append(hashString).append("\" is \"");
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            response.setStatus(500);
            return "Failed to configure the message digest: " + e;
        }
        final byte[] digestBytes = md.digest(hashString.getBytes(StandardCharsets.UTF_8));
        b.append(Base64.getEncoder().withoutPadding().encodeToString(digestBytes));
        return b.toString();
    }
}
