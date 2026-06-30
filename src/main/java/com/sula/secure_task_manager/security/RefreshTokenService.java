package com.sula.secure_task_manager.security;

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

    public void store(String refreshToken) {
        String tokenId = jwtService.extractTokenId(refreshToken);
        Duration ttl = Duration.between(Instant.now(), jwtService.extractExpiration(refreshToken).toInstant());
        redisTemplate.opsForValue().set(key(tokenId), refreshToken, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    public boolean isActive(String refreshToken) {
        String tokenId = jwtService.extractTokenId(refreshToken);
        String storedToken = redisTemplate.opsForValue().get(key(tokenId));
        return refreshToken.equals(storedToken);
    }

    public void revoke(String refreshToken) {
        String tokenId = jwtService.extractTokenId(refreshToken);
        redisTemplate.delete(key(tokenId));
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}
