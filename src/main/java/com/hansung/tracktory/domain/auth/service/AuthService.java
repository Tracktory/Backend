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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_DUPLICATE);
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(new UserPrincipal(savedUser));

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                token,
                "Bearer",
                jwtUtil.getExpirationSeconds()
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtUtil.generateToken(new UserPrincipal(user));
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                token,
                "Bearer",
                jwtUtil.getExpirationSeconds(),
                true
        );
    }
}
