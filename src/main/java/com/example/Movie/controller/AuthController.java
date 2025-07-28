package com.example.Movie.controller;

import com.example.Movie.dto.RegisterRequest;
import com.example.Movie.dto.LoginRequest;
import com.example.Movie.model.User;
import com.example.Movie.repository.UserRepository;
import com.example.Movie.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        String token = jwtUtils.generateToken(user);

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // Set to false for local dev without HTTPS
                .path("/")
                .maxAge(24 * 60 * 60) // 1 day expiration
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("Registration successful");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty() ||
                !passwordEncoder.matches(request.getPassword(), optionalUser.get().getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid email or password.");
        }

        String token = jwtUtils.generateToken(optionalUser.get());

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // Set to false for local dev without HTTPS
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("Login successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true) // match the flags of your other cookies
                .path("/")
                .maxAge(0) // deletes cookie immediately
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/check")
    public ResponseEntity<?> check(Principal principal) {
        if (principal == null || principal.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        // Optional: check if user exists in DB for extra safety
        Optional<User> user = userRepository.findByEmail(principal.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }

        return ResponseEntity.ok().body(Map.of("user", principal.getName()));
    }
}
