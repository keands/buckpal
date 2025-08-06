package com.buckpal.controller;

import com.buckpal.dto.JwtResponse;
import com.buckpal.dto.LoginRequest;
import com.buckpal.dto.UserRegistrationDto;
import com.buckpal.entity.User;
import com.buckpal.repository.UserRepository;
import com.buckpal.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {AuthController.class})
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthenticationManager authenticationManager;
    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private PasswordEncoder passwordEncoder;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    private LoginRequest loginRequest;
    private UserRegistrationDto registrationDto;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword("encodedPassword");
        
        loginRequest = new LoginRequest("john.doe@example.com", "password");
        
        registrationDto = new UserRegistrationDto();
        registrationDto.setFirstName("John");
        registrationDto.setLastName("Doe");
        registrationDto.setEmail("john.doe@example.com");
        registrationDto.setPassword("password123");
    }
    
    @Test
    void shouldAuthenticateUserWithValidCredentials() throws Exception {
        Authentication mockAuthentication = new UsernamePasswordAuthenticationToken(testUser, null);
        String expectedToken = "jwt.token.here";
        
        when(authenticationManager.authenticate(any())).thenReturn(mockAuthentication);
        when(jwtTokenProvider.generateJwtToken(mockAuthentication)).thenReturn(expectedToken);
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(expectedToken))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
    
    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new RuntimeException("Invalid credentials"));
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
    
    @Test
    void shouldRegisterNewUser() throws Exception {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }
    
    @Test
    void shouldRejectRegistrationWithExistingEmail() throws Exception {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldValidateRegistrationInput() throws Exception {
        UserRegistrationDto invalidDto = new UserRegistrationDto();
        invalidDto.setFirstName(""); // Invalid: empty
        invalidDto.setLastName("Doe");
        invalidDto.setEmail("invalid-email"); // Invalid: not an email
        invalidDto.setPassword("123"); // Invalid: too short
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldValidateLoginInput() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", ""); // Both empty
        
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}