package com.falconenergy.service.impl;

import com.falconenergy.service.PawaPayCallbackVerifier;
import com.falconenergy.service.PawaPayClient;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verifies pawaPay's RFC 9421 signed callback before any payment state is changed. */
@Component @RequiredArgsConstructor
public class PawaPayCallbackVerifierImpl implements PawaPayCallbackVerifier {
    private static final Pattern DIGEST = Pattern.compile("(?i)(sha-256|sha-512)=:([^:]+):");
    private static final Pattern SIGNATURE = Pattern.compile("(?:^|,)\\s*sig-pp=:([^:]+):");
    private static final Pattern SIGNATURE_INPUT = Pattern.compile("^sig-pp=\\(\\\"@method\\\" \\\"@authority\\\" \\\"@path\\\" \\\"signature-date\\\" \\\"content-digest\\\" \\\"content-type\\\"\\);.*$");
    private static final Pattern KEY_ID = Pattern.compile("keyid=\\\"([^\\\"]+)\\\"");
    private final PawaPayClient pawaPay;
    private final ConcurrentHashMap<String, PublicKey> keys = new ConcurrentHashMap<>();

    @Override public void verify(HttpServletRequest request, byte[] body) {
        try {
            String contentDigest = required(request, "Content-Digest");
            verifyDigest(contentDigest, body);
            String signatureInput = required(request, "Signature-Input");
            if (!SIGNATURE_INPUT.matcher(signatureInput).matches()) reject();
            String signatureDate = required(request, "Signature-Date");
            if (Duration.between(Instant.parse(signatureDate), Instant.now()).abs().compareTo(Duration.ofMinutes(5)) > 0) reject();
            Matcher signature = SIGNATURE.matcher(required(request, "Signature"));
            if (!signature.find()) reject();
            String authority = required(request, "Host");
            String contentType = required(request, "Content-Type");
            String base = "\"@method\": " + request.getMethod() + "\n"
                    + "\"@authority\": " + authority + "\n"
                    + "\"@path\": " + request.getRequestURI() + "\n"
                    + "\"signature-date\": " + signatureDate + "\n"
                    + "\"content-digest\": " + contentDigest + "\n"
                    + "\"content-type\": " + contentType + "\n"
                    + "\"@signature-params\": " + signatureInput;
            Matcher keyId=KEY_ID.matcher(signatureInput); if(!keyId.find()) reject();
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(keys.computeIfAbsent(keyId.group(1), this::loadPawaPayPublicKey)); verifier.update(base.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signature.group(1)))) reject();
        } catch (ResponseStatusException exception) { throw exception;
        } catch (Exception exception) { reject(); }
    }

    private void verifyDigest(String value, byte[] body) throws Exception {
        Matcher digest = DIGEST.matcher(value); if (!digest.find()) reject();
        MessageDigest algorithm = MessageDigest.getInstance("sha-256".equalsIgnoreCase(digest.group(1)) ? "SHA-256" : "SHA-512");
        if (!MessageDigest.isEqual(algorithm.digest(body), Base64.getDecoder().decode(digest.group(2)))) reject();
    }
    private PublicKey loadPawaPayPublicKey(String keyId) { try { String pem=pawaPay.publicKeyPem(keyId); String encoded=pem.replaceAll("-----BEGIN PUBLIC KEY-----|-----END PUBLIC KEY-----|\\s",""); return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded))); } catch(Exception exception){ throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"pawaPay callback verification key is unavailable."); } }
    private String required(HttpServletRequest request, String header) { String value=request.getHeader(header); if(value==null||value.isBlank()) reject(); return value; }
    private void reject() { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid pawaPay callback signature."); }
}
