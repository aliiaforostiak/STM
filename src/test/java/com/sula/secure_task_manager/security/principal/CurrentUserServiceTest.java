package com.sula.secure_task_manager.security.principal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sula.secure_task_manager.manager.user.Role;

class CurrentUserServiceTest {

    private final CurrentUserService currentUserService = new CurrentUserService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class GetCurrentUserId {

        @Test
        void shouldReturnCurrentUserId_whenPrincipalIsCustomUserDetails() {
            CustomUserDetails principal = new CustomUserDetails(
                    42L,
                    "anna@example.com",
                    "encoded-password",
                    Role.USER,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            Long currentUserId = currentUserService.getCurrentUserId();

            assertThat(currentUserId).isEqualTo(42L);
        }

        @Test
        void shouldThrowIllegalStateException_whenAuthenticationIsMissing() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(currentUserService::getCurrentUserId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("User is not authenticated");
        }

        @Test
        void shouldThrow_whenPrincipalIsUnsupported() {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "test@example.com",
                            null,
                            List.of()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThatThrownBy(currentUserService::getCurrentUserId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unsupported authentication principal");
        }
    }
}
