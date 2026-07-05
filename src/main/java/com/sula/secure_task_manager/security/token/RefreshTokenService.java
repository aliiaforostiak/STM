package com.sula.secure_task_manager.security.token;

import com.sula.secure_task_manager.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "secure-task-manager:refresh-token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;

    public void store(String refreshToken) {
        String tokenId = jwtService.extractTokenId(refreshToken);
        String tokenHash = tokenHashService.sha256(refreshToken);

        Duration ttl = Duration.between(
                Instant.now(),
                jwtService.extractExpiration(refreshToken).toInstant()
        );

        long ttlMillis = ttl.toMillis();
        if (ttlMillis <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                key(tokenId),
                tokenHash,
                ttlMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isActive(String refreshToken) {
        String tokenId = jwtService.extractTokenId(refreshToken);
        String storedHash = redisTemplate.opsForValue().get(key(tokenId));

        if (storedHash == null) {
            return false;
        }

        String currentHash = tokenHashService.sha256(refreshToken);

        return currentHash.equals(storedHash);
    }

    public void revoke(String refreshToken) {
        String tokenId = jwtService.extractTokenId(refreshToken);
        redisTemplate.delete(key(tokenId));
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}
