package com.hansung.tracktory.domain.user.service;

import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.domain.user.repository.UserRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsername_success() {
        given(userRepository.findByEmail("a@b.com"))
                .willReturn(Optional.of(User.builder().email("a@b.com").passwordHash("hashed").build()));

        UserDetails result = customUserDetailsService.loadUserByUsername("a@b.com");

        assertThat(result.getUsername()).isEqualTo("a@b.com");
    }

    @Test
    void loadUserByUsername_normalizes_email_before_lookup() {
        given(userRepository.findByEmail("a@b.com"))
                .willReturn(Optional.of(User.builder().email("a@b.com").passwordHash("hashed").build()));

        UserDetails result = customUserDetailsService.loadUserByUsername(" A@B.COM ");

        verify(userRepository).findByEmail("a@b.com");
        assertThat(result.getUsername()).isEqualTo("a@b.com");
    }

    @Test
    void loadUserByUsername_unknown_user_throws_exception() {
        given(userRepository.findByEmail("none@b.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("none@b.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
