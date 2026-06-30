package com.sula.secure_task_manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sula.secure_task_manager.auth.controller.AuthController;
import com.sula.secure_task_manager.auth.dto.LoginResponse;
import com.sula.secure_task_manager.auth.dto.RegisterResponse;
import com.sula.secure_task_manager.auth.dto.RegisterRequest;
import com.sula.secure_task_manager.auth.dto.LoginRequest;
import com.sula.secure_task_manager.auth.service.AuthService;
import com.sula.secure_task_manager.common.exception.GlobalExceptionHandler;
import com.sula.secure_task_manager.common.exception.UserAlreadyExistsException;
import com.sula.secure_task_manager.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void register_returnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");
        when(authService.register(request)).thenReturn(new RegisterResponse(1L, "test@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_returnsTokens() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(authService.login(request)).thenReturn(new LoginResponse("access", "refresh"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    void register_returnsConflictOnDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");
        when(authService.register(request)).thenThrow(new UserAlreadyExistsException("User with this email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("User with this email already exists"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].field").value("email"));
    }

    @Test
    void register_returnsValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest("bad", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].field").exists());
    }
}
