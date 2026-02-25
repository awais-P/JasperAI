package com.awais.jasperAi.controllers;

import com.awais.jasperAi.entities.User;
import com.awais.jasperAi.repositories.UserRepository;
import com.awais.jasperAi.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String fullName = payload.get("fullName");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }

        // HASH THE PASSWORD BEFORE SAVING
        User newUser = new User(email, passwordEncoder.encode(password), fullName);
        userRepository.save(newUser);

        return ResponseEntity.ok(Map.of("message", "User registered successfully", "plan", newUser.getPlanType()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        // 1. Authenticate credentials (this checks the hashed password automatically)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        // 2. Load user and generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        User dbUser = userRepository.findByEmail(email).get();
        String jwtToken = jwtService.generateToken(userDetails);

        // 3. Return the token and user info
        return ResponseEntity.ok(Map.of(
                "token", jwtToken,
                "email", dbUser.getEmail(),
                "fullName", dbUser.getFullName(),
                "planType", dbUser.getPlanType()
        ));
    }
}