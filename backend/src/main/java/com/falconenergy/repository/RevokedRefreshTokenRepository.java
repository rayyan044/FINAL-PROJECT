package com.falconenergy.repository;

import com.falconenergy.entity.RevokedRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface RevokedRefreshTokenRepository extends JpaRepository<RevokedRefreshToken, Long> {
    boolean existsByTokenHash(String tokenHash);
    void deleteByExpiresAtBefore(Instant instant);
}
