package com.falconenergy.security;

import com.falconenergy.entity.RevokedRefreshToken;
import com.falconenergy.repository.RevokedRefreshTokenRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenRevocationService {
    private final RevokedRefreshTokenRepository repository;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public void revoke(String token) {
        Claims claims = tokenProvider.extractAllClaimsForRevocation(token);
        repository.save(new RevokedRefreshToken(hash(token), claims.getExpiration().toInstant()));
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) { return repository.existsByTokenHash(hash(token)); }

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void purgeExpired() { repository.deleteByExpiresAtBefore(Instant.now()); }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
