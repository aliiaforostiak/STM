package com.sula.secure_task_manager.auth.service;

import com.sula.secure_task_manager.auth.dto.LoginRequest;
import com.sula.secure_task_manager.auth.dto.LoginResponse;
import com.sula.secure_task_manager.auth.dto.MeResponse;
import com.sula.secure_task_manager.auth.dto.RefreshTokenRequest;
import com.sula.secure_task_manager.auth.dto.RegisterRequest;
import com.sula.secure_task_manager.auth.dto.RegisterResponse;
import com.sula.secure_task_manager.common.exception.UserAlreadyExistsException;
import com.sula.secure_task_manager.security.JwtService;
import com.sula.secure_task_manager.security.RefreshTokenService;
import com.sula.secure_task_manager.user.User;
import com.sula.secure_task_manager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        User user = User.create(
                request.email(),
                passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return issueTokenPair(user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new MeResponse(user.getId(), user.getEmail(), user.getRole());
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isRefreshTokenValid(refreshToken) || !refreshTokenService.isActive(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(jwtService.extractEmail(refreshToken))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        refreshTokenService.revoke(refreshToken);
        return issueTokenPair(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isRefreshTokenValid(refreshToken) || !refreshTokenService.isActive(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        refreshTokenService.revoke(refreshToken);
    }

    private LoginResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.store(refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }
}
