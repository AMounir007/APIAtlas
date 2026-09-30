package com.apiatlas.controller;

import com.apiatlas.dto.LoginRequest;
import com.apiatlas.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserDetailsService users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        try {
            UserDetails u = users.loadUserByUsername(req.username());
            if (encoder.matches(req.password(), u.getPassword())) {
                return ResponseEntity.ok(Map.of("token", jwt.generate(u.getUsername()),
                        "expiresInSeconds", jwt.expiration().toSeconds()));
            }
        } catch (UsernameNotFoundException ignored) {
            // fall through to generic failure - do not reveal whether the user exists
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }
}
