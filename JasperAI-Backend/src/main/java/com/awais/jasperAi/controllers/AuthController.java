package com.awais.jasperAi.controllers;

import com.awais.jasperAi.entities.User;
import com.awais.jasperAi.repositories.UserRepository;
import com.awais.jasperAi.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

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
        try {
            String email = payload.get("email");
            String password = payload.get("password");
            String fullName = payload.get("fullName");

            // Check if email is already taken
            if (userRepository.existsByEmail(email)) {
                // Return 409 Conflict with JSON error
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email is already in use. Please log in."));
            }

            // Hash password and save
            User newUser = new User(email, passwordEncoder.encode(password), fullName);
            userRepository.save(newUser);

            // Return 201 Created
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "User registered successfully",
                            "plan", newUser.getPlanType()
                    ));

        } catch (Exception e) {
            // Generic 500 fallback
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while creating the account."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String password = payload.get("password");

            // 1. Authenticate credentials
            // If the password is wrong or email doesn't exist, this THROWS an exception.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // 2. If we reach here, credentials are correct. Load user and generate token.
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            Optional<User> dbUserOptional = userRepository.findByEmail(email);

            if (dbUserOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User account not found."));
            }

            User dbUser = dbUserOptional.get();
            String jwtToken = jwtService.generateToken(userDetails);

            // 3. Return 200 OK with the token and user info
            return ResponseEntity.ok(Map.of(
                    "token", jwtToken,
                    "email", dbUser.getEmail(),
                    "fullName", dbUser.getFullName(),
                    "planType", dbUser.getPlanType()
            ));

        } catch (BadCredentialsException e) {
            // CATCH WRONG PASSWORD OR EMAIL
            // Return 401 Unauthorized with the exact JSON structure Angular expects
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password. Please try again."));

        } catch (AuthenticationException e) {
            // Catch any other Spring Security auth issues (like locked accounts)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed."));

        } catch (Exception e) {
            // Generic 500 fallback
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected server error occurred."));
        }
    }
}