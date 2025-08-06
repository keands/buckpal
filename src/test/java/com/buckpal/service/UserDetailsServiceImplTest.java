package com.buckpal.service;

import com.buckpal.entity.User;
import com.buckpal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);
    }
    
    @Test
    void shouldLoadUserByUsernameWhenUserExists() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername("john.doe@example.com");
        
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("john.doe@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        
        verify(userRepository).findByEmail("john.doe@example.com");
    }
    
    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistentEmail))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User Not Found with email: " + nonExistentEmail);
        
        verify(userRepository).findByEmail(nonExistentEmail);
    }
    
    @Test
    void shouldReturnUserWithCorrectAuthorities() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername("john.doe@example.com");
        
        assertThat(userDetails.getAuthorities()).isNotNull();
        assertThat(userDetails.getAuthorities()).isEmpty(); // Default implementation returns empty list
    }
    
    @Test
    void shouldHandleDisabledUser() {
        testUser.setEnabled(false);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername("john.doe@example.com");
        
        assertThat(userDetails.isEnabled()).isFalse();
    }
    
    @Test
    void shouldHandleExpiredAccount() {
        testUser.setAccountNonExpired(false);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername("john.doe@example.com");
        
        assertThat(userDetails.isAccountNonExpired()).isFalse();
    }
    
    @Test
    void shouldHandleLockedAccount() {
        testUser.setAccountNonLocked(false);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername("john.doe@example.com");
        
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }
    
    @Test
    void shouldHandleExpiredCredentials() {
        testUser.setCredentialsNonExpired(false);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        
        UserDetails userDetails = userDetailsService.loadUserByUsername("john.doe@example.com");
        
        assertThat(userDetails.isCredentialsNonExpired()).isFalse();
    }
}