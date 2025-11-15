package com.expense_tracker.controller;

import com.expense_tracker.dto.AuthResponseDTO;
import com.expense_tracker.model.RefreshToken;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.UserRepository;
import com.expense_tracker.response.ApiResponse;
import com.expense_tracker.security.JwtService;
import com.expense_tracker.service.RefreshTokenService;
import com.expense_tracker.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostConstruct
    public void init() {
        System.out.println("AuthController loaded!");
    }


    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerUser(@RequestBody User user) {
        System.out.println("Enter in the Register endpoint");
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>("error", "Email already exists", null, 400));
        }

        userService.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("success", "User registered successfully", null, HttpStatus.CREATED.value()));

    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@RequestBody User loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateToken(userDetails.getUsername()); // username == email


        // create refresh token and return both
        // find user by email
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        AuthResponseDTO authResponse = new AuthResponseDTO(accessToken, refreshToken.getToken(),
                jwtService.getExpiryInSeconds());

        ApiResponse<AuthResponseDTO> response = new ApiResponse<>(
                "success",
                "Login successful",
                authResponse,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    //refresh
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshToken(
            @RequestBody Map<String, String> request) {
        String requestRefreshToken = request.get("refreshToken");

        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "error",
                    "refreshToken is required",
                    null,
                    HttpStatus.BAD_REQUEST.value()
            ));
        }

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(rt -> {
                    if (!refreshTokenService.verifyExpiration(rt)) {
                        // revoke & delete or just respond with expired
                        refreshTokenService.revoke(rt);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new ApiResponse<AuthResponseDTO>(
                                        "error",
                                        "Refresh token expired",
                                        null,
                                        HttpStatus.UNAUTHORIZED.value()
                                ));
                    }

                    // Rotate refresh token : revoke old and issue new

                    refreshTokenService.revoke(rt);



                    User user = rt.getUser();

                    // delete all old tokens
//                    refreshTokenService.revokeAllTokensForUser(user);

                    RefreshToken newRt = refreshTokenService.createRefreshToken(user.getId());


                    // create new access token
                    String newAccessToken = jwtService.generateToken(user.getEmail());
                    AuthResponseDTO authResponse = new AuthResponseDTO(newAccessToken, newRt.getToken(),
                            jwtService.getExpiryInSeconds());
                    return ResponseEntity.ok(new ApiResponse<>(
                                    "success",
                                    "Token refreshed",
                                    authResponse,
                                    HttpStatus.OK.value()
                            )
                    );
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<AuthResponseDTO>(
                                "error",
                                "Invalid refresh token",
                                null,
                                HttpStatus.UNAUTHORIZED.value())
                        )
                );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody Map<String, String> request
    ) {
        String refresh = request.get("refreshToken");
        if (refresh != null) {
            refreshTokenService.findByToken(refresh).ifPresent(refreshTokenService::revoke);
        }

        // optionally clear client cookies etc.
        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Logged out",
                null,
                HttpStatus.OK.value()
        ));
    }


}