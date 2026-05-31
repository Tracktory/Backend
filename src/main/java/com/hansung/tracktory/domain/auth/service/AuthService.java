package com.hansung.tracktory.domain.auth.service;

import com.hansung.tracktory.domain.auth.dto.LoginRequest;
import com.hansung.tracktory.domain.auth.dto.LoginResponse;
import com.hansung.tracktory.domain.auth.dto.SignupRequest;
import com.hansung.tracktory.domain.auth.dto.SignupResponse;
import com.hansung.tracktory.domain.profile.repository.UserProfileRepository;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.domain.user.repository.UserRepository;
import com.hansung.tracktory.domain.user.service.UserEmailNormalizer;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import com.hansung.tracktory.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;

  public SignupResponse signup(SignupRequest request) {
    String email = UserEmailNormalizer.normalize(request.email());

    if (userRepository.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATE);
    }

    User user =
        User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();

    User savedUser;
    try {
      savedUser = userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATE);
    }
    String token = jwtUtil.generateToken(new UserPrincipal(savedUser));

    return new SignupResponse(
        savedUser.getId(), savedUser.getEmail(), token, "Bearer", jwtUtil.getExpirationSeconds());
  }

  public LoginResponse login(LoginRequest request) {
    String email = UserEmailNormalizer.normalize(request.email());
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(email, request.password()));
    } catch (AuthenticationException e) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    String token = jwtUtil.generateToken(principal);
    boolean onboardingCompleted = userProfileRepository.existsByUserId(principal.getUserId());
    return new LoginResponse(
        principal.getUserId(),
        principal.getUsername(),
        token,
        "Bearer",
        jwtUtil.getExpirationSeconds(),
        onboardingCompleted);
  }
}
