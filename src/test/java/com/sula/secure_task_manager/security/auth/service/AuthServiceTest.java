package com.sula.secure_task_manager.security.auth.service;

import com.sula.secure_task_manager.security.auth.dto.LoginRequest;
import com.sula.secure_task_manager.security.auth.dto.RefreshTokenRequest;
import com.sula.secure_task_manager.security.auth.dto.RegisterRequest;
import com.sula.secure_task_manager.security.jwt.JwtService;
import com.sula.secure_task_manager.security.token.RefreshTokenService;
import com.sula.secure_task_manager.manager.user.Role;
import com.sula.secure_task_manager.manager.user.User;
import com.sula.secure_task_manager.manager.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsUser() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");
        User savedUser = User.create(request.email(), "encoded-password");
        ReflectionTestUtils.setField(savedUser, "id", 1L);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = authService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void login_returnsTokenPair() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User user = User.create(request.email(), "encoded-password");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        var response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenService).store("refresh-token");
    }

    @Test
    void refresh_rotatesTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        User user = User.create("test@example.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "role", Role.USER);

        when(jwtService.isRefreshTokenValid("refresh-token")).thenReturn(true);
        when(refreshTokenService.isActive("refresh-token")).thenReturn(true);
        when(jwtService.extractEmail("refresh-token")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

        var response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService).revoke("refresh-token");
        verify(refreshTokenService).store("new-refresh-token");
    }

    @Test
    void logout_revokesRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(jwtService.isRefreshTokenValid("refresh-token")).thenReturn(true);
        when(refreshTokenService.isActive("refresh-token")).thenReturn(true);

        authService.logout(request);

        verify(refreshTokenService).revoke("refresh-token");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void me_returnsCurrentUser() {
        User user = User.create("test@example.com", "encoded-password");
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "role", Role.ADMIN);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        var response = authService.me("test@example.com");

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void refresh_rejectsInvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(jwtService.isRefreshTokenValid("refresh-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }
}
