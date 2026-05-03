package com.ethara.taskmanager.service;

import com.ethara.taskmanager.dto.auth.AuthResponse;
import com.ethara.taskmanager.dto.auth.LoginRequest;
import com.ethara.taskmanager.dto.auth.LoginType;
import com.ethara.taskmanager.dto.auth.SignupRequest;
import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.Role;
import com.ethara.taskmanager.exception.DuplicateResourceException;
import com.ethara.taskmanager.exception.ForbiddenException;
import com.ethara.taskmanager.repository.UserRepository;
import com.ethara.taskmanager.security.AuthenticatedUser;
import com.ethara.taskmanager.security.JwtService;
import com.ethara.taskmanager.service.support.ResponseMapper;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ResponseMapper responseMapper;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.MEMBER);

        User savedUser = userRepository.save(user);
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromUser(savedUser);

        return AuthResponse.builder()
            .accessToken(jwtService.generateToken(authenticatedUser))
            .tokenType(TOKEN_TYPE)
            .user(responseMapper.toUserSummary(savedUser))
            .build();
    }

    public AuthResponse login(LoginRequest request, LoginType loginType) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        User user = userRepository.findById(authenticatedUser.getId())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getRole() != loginType.getRequiredRole()) {
            throw buildRoleMismatchException(loginType);
        }

        return AuthResponse.builder()
            .accessToken(jwtService.generateToken(authenticatedUser))
            .tokenType(TOKEN_TYPE)
            .user(responseMapper.toUserSummary(user))
            .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ForbiddenException buildRoleMismatchException(LoginType loginType) {
        if (loginType == LoginType.ADMIN) {
            return new ForbiddenException("You are not authorized to login as admin");
        }

        return new ForbiddenException("You are not authorized to login as member");
    }
}
