package com.hansung.tracktory.domain.auth.service;

import com.hansung.tracktory.domain.auth.dto.LoginRequest;
import com.hansung.tracktory.domain.auth.dto.LoginResponse;
import com.hansung.tracktory.domain.auth.dto.SignupRequest;
import com.hansung.tracktory.domain.auth.dto.SignupResponse;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.domain.user.repository.UserRepository;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import com.hansung.tracktory.global.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @Test
    void signup_success() {
        given(userRepository.existsByEmail("a@b.com")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        given(userRepository.save(any(User.class)))
                .willReturn(User.builder().email("a@b.com").passwordHash("hashed").build());
        given(jwtUtil.generateToken(any(UserPrincipal.class))).willReturn("token");

        SignupResponse result = authService.signup(new SignupRequest("a@b.com", "password1"));

        assertThat(result.email()).isEqualTo("a@b.com");
        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void signup_normalizes_email_before_lookup_and_save() {
        given(userRepository.existsByEmail("a@b.com")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        given(userRepository.save(any(User.class)))
                .willReturn(User.builder().email("a@b.com").passwordHash("hashed").build());
        given(jwtUtil.generateToken(any(UserPrincipal.class))).willReturn("token");

        SignupResponse result = authService.signup(new SignupRequest(" A@B.COM ", "password1"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByEmail("a@b.com");
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(result.email()).isEqualTo("a@b.com");
    }

    @Test
    void signup_duplicate_email_throws_business_exception() {
        given(userRepository.existsByEmail("a@b.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("a@b.com", "password1")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_EMAIL_DUPLICATE));
    }

    @Test
    void signup_concurrent_duplicate_email_throws_business_exception() {
        given(userRepository.existsByEmail("a@b.com")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        given(userRepository.save(any(User.class))).willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.signup(new SignupRequest("a@b.com", "password1")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_EMAIL_DUPLICATE));
    }

    @Test
    void login_success() {
        UserPrincipal principal = UserPrincipal.of(1L, "a@b.com");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        given(authenticationManager.authenticate(any())).willReturn(auth);
        given(jwtUtil.generateToken(any(UserPrincipal.class))).willReturn("token");

        LoginResponse result = authService.login(new LoginRequest("a@b.com", "password1"));

        assertThat(result.email()).isEqualTo("a@b.com");
        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_normalizes_email_before_authentication() {
        UserPrincipal principal = UserPrincipal.of(1L, "a@b.com");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        given(authenticationManager.authenticate(any())).willReturn(auth);
        given(jwtUtil.generateToken(any(UserPrincipal.class))).willReturn("token");

        authService.login(new LoginRequest(" A@B.COM ", "password1"));

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertThat(authCaptor.getValue().getName()).isEqualTo("a@b.com");
        assertThat(authCaptor.getValue().getCredentials()).isEqualTo("password1");
    }

    @Test
    void login_unknown_email_throws_business_exception() {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@b.com", "password1")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void login_wrong_password_throws_business_exception() {
        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrongpw")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}
