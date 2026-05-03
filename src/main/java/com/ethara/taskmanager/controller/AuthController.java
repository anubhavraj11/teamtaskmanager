package com.ethara.taskmanager.controller;

import com.ethara.taskmanager.dto.auth.AuthResponse;
import com.ethara.taskmanager.dto.auth.LoginRequest;
import com.ethara.taskmanager.dto.auth.LoginType;
import com.ethara.taskmanager.dto.auth.SignupRequest;
import com.ethara.taskmanager.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @RequestParam("type") String type,
        @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request, LoginType.from(type)));
    }
}
