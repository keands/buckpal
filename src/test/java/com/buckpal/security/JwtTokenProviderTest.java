package com.buckpal.security;

import com.buckpal.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private Authentication authentication;
    
    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        
        // Set test values using reflection
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "mySecretKeyForTestingPurposesOnly1234567890");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password");
        
        authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
    }
    
    @Test
    void shouldGenerateJwtToken() {
        String token = jwtTokenProvider.generateJwtToken(authentication);
        
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
    }
    
    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtTokenProvider.generateJwtToken(authentication);
        
        String username = jwtTokenProvider.getUsernameFromJwtToken(token);
        
        assertThat(username).isEqualTo(testUser.getEmail());
    }
    
    @Test
    void shouldValidateValidToken() {
        String token = jwtTokenProvider.generateJwtToken(authentication);
        
        boolean isValid = jwtTokenProvider.validateJwtToken(token);
        
        assertThat(isValid).isTrue();
    }
    
    @Test
    void shouldRejectInvalidToken() {
        String invalidToken = "invalid.jwt.token";
        
        boolean isValid = jwtTokenProvider.validateJwtToken(invalidToken);
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        // Set very short expiration time
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 1);
        
        String token = jwtTokenProvider.generateJwtToken(authentication);
        
        // Wait for token to expire
        Thread.sleep(10);
        
        boolean isValid = jwtTokenProvider.validateJwtToken(token);
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void shouldRejectMalformedToken() {
        String malformedToken = "malformed-token-without-proper-structure";
        
        boolean isValid = jwtTokenProvider.validateJwtToken(malformedToken);
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void shouldRejectEmptyToken() {
        boolean isValid = jwtTokenProvider.validateJwtToken("");
        
        assertThat(isValid).isFalse();
    }
    
    @Test
    void shouldRejectNullToken() {
        boolean isValid = jwtTokenProvider.validateJwtToken(null);
        
        assertThat(isValid).isFalse();
    }
}