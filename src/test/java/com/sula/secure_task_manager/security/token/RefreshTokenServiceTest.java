package com.sula.secure_task_manager.security.token;

import com.sula.secure_task_manager.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String TOKEN_ID = "token-123";
    private static final String KEY = "secure-task-manager:refresh-token:" + TOKEN_ID;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Captor
    private ArgumentCaptor<Long> ttlCaptor;

    @Test
    void store_shouldSaveTokenWithExpectedKeyAndTtl() {
        Date expiration = Date.from(Instant.now().plusSeconds(120));
        String tokenHash = "hashed-token";

        when(jwtService.extractTokenId(REFRESH_TOKEN)).thenReturn(TOKEN_ID);
        when(jwtService.extractExpiration(REFRESH_TOKEN)).thenReturn(expiration);
        when(tokenHashService.sha256(REFRESH_TOKEN)).thenReturn(tokenHash);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenService.store(REFRESH_TOKEN);

        verify(jwtService).extractTokenId(REFRESH_TOKEN);
        verify(jwtService).extractExpiration(REFRESH_TOKEN);
        verify(tokenHashService).sha256(REFRESH_TOKEN);
        verify(valueOperations).set(eq(KEY), eq(tokenHash), ttlCaptor.capture(), eq(TimeUnit.MILLISECONDS));
        assertTrue(ttlCaptor.getValue() > 0);
        assertTrue(ttlCaptor.getValue() <= 120_000L);
    }

    @Test
    void store_shouldNotSaveToken_whenTokenExpired() {
        Date expiration = Date.from(Instant.now().minusSeconds(120));

        when(jwtService.extractTokenId(REFRESH_TOKEN)).thenReturn(TOKEN_ID);
        when(jwtService.extractExpiration(REFRESH_TOKEN)).thenReturn(expiration);
        when(tokenHashService.sha256(REFRESH_TOKEN)).thenReturn("hashed-token");

        refreshTokenService.store(REFRESH_TOKEN);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void isActive_shouldReturnTrue_whenStoredTokenMatches() {
        String tokenHash = "hashed-token";

        when(jwtService.extractTokenId(REFRESH_TOKEN)).thenReturn(TOKEN_ID);
        when(tokenHashService.sha256(REFRESH_TOKEN)).thenReturn(tokenHash);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(tokenHash);

        boolean result = refreshTokenService.isActive(REFRESH_TOKEN);

        assertTrue(result);
        verify(jwtService).extractTokenId(REFRESH_TOKEN);
        verify(tokenHashService).sha256(REFRESH_TOKEN);
        verify(valueOperations).get(KEY);
    }

    @Test
    void isActive_shouldReturnFalse_whenStoredTokenIsMissing() {
        when(jwtService.extractTokenId(REFRESH_TOKEN)).thenReturn(TOKEN_ID);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        boolean result = refreshTokenService.isActive(REFRESH_TOKEN);

        assertFalse(result);
        verify(jwtService).extractTokenId(REFRESH_TOKEN);
        verify(valueOperations).get(KEY);
    }

    @Test
    void isActive_shouldReturnFalse_whenStoredTokenDiffers() {
        String tokenHash = "hashed-token";

        when(jwtService.extractTokenId(REFRESH_TOKEN)).thenReturn(TOKEN_ID);
        when(tokenHashService.sha256(REFRESH_TOKEN)).thenReturn(tokenHash);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("another-hash");

        boolean result = refreshTokenService.isActive(REFRESH_TOKEN);

        assertFalse(result);
        verify(jwtService).extractTokenId(REFRESH_TOKEN);
        verify(tokenHashService).sha256(REFRESH_TOKEN);
        verify(valueOperations).get(KEY);
    }

    @Test
    void revoke_shouldDeleteTokenByExpectedKey() {
        when(jwtService.extractTokenId(REFRESH_TOKEN)).thenReturn(TOKEN_ID);

        refreshTokenService.revoke(REFRESH_TOKEN);

        verify(jwtService).extractTokenId(REFRESH_TOKEN);
        verify(redisTemplate).delete(KEY);
    }

}
