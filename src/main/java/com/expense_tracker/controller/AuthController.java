package com.expense_tracker.controller;

import com.expense_tracker.dto.AuthResponse;
import com.expense_tracker.dto.LoginRequest;
import com.expense_tracker.dto.UserRequestDTO;
import com.expense_tracker.dto.UserResponseDTO;
import com.expense_tracker.model.Role;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.UserRepository;
import com.expense_tracker.response.ApiResponse;
import com.expense_tracker.service.AuthService;
import com.expense_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerUser(
            @Valid @RequestBody UserRequestDTO request) {
        System.out.println("Enter in the Register endpoint AuthController -> register method");

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            "error",
                            "Email already exists",
                            null,
                            400)
                    );
        }

        // Map DTO -> Entity
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // will be encoded in service
        user.setRole(Role.USER); // default role


        User savedUser = userService.saveUser(user);

        // Map Entity -> Response DTO
        UserResponseDTO responseDTO = new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
        responseDTO.setCreatedAt(savedUser.getCreatedAt().toLocalDate()); // <-- add this


        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "success",
                        "User registered successfully",
                        responseDTO,
                        HttpStatus.CREATED.value())
                );
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        "success",
                        "Login successful",
                        response,
                        HttpStatus.OK.value()
                )
        );
    }


    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "error",
                    "refreshToken is required",
                    null,
                    400)
            );
        }
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Token refreshed",
                response,
                200)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    "error",
                    "refreshToken is required",
                    null,
                    400)
            );
        }
        authService.logout(refreshToken);
        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Logged out successfully",
                null,
                200)
        );
    }


//    @PostMapping("/login")
//    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody User loginRequest) {
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
//
//        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
//
//        String accessToken = jwtService.generateToken(userDetails.getUsername()); // username == email
//
//
//        // create refresh token and return both
//        // find user by email
//        User user = userRepository.findByEmail(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
//
//        AuthResponse authResponse = new AuthResponse(accessToken, refreshToken.getToken(),
//                jwtService.getExpiryInSeconds());
//
//        ApiResponse<AuthResponse> response = new ApiResponse<>(
//                "success",
//                "Login successful",
//                authResponse,
//                HttpStatus.OK.value()
//        );
//        return ResponseEntity.ok(response);
//    }


    //refresh
//    @PostMapping("/refresh")
//    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
//            @RequestBody Map<String, String> request) {
//        String requestRefreshToken = request.get("refreshToken");
//
//        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
//            return ResponseEntity.badRequest().body(new ApiResponse<>(
//                    "error",
//                    "refreshToken is required",
//                    null,
//                    HttpStatus.BAD_REQUEST.value()
//            ));
//        }
//
//        return refreshTokenService.findByToken(requestRefreshToken)
//                .map(rt -> {
//                    if (!refreshTokenService.verifyExpiration(rt)) {
//                        // revoke & delete or just respond with expired
//                        refreshTokenService.revoke(rt);
//                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                                .body(new ApiResponse<AuthResponse>(
//                                        "error",
//                                        "Refresh token expired",
//                                        null,
//                                        HttpStatus.UNAUTHORIZED.value()
//                                ));
//                    }
//
//                    // Rotate refresh token : revoke old and issue new
//
//                    refreshTokenService.revoke(rt);
//
//
//
//                    User user = rt.getUser();
//
//                    // delete all old tokens
////                    refreshTokenService.revokeAllTokensForUser(user);
//
//                    RefreshToken newRt = refreshTokenService.createRefreshToken(user.getId());
//
//
//                    // create new access token
//                    String newAccessToken = jwtService.generateToken(user.getEmail());
//                    AuthResponse authResponse = new AuthResponse(newAccessToken, newRt.getToken(),
//                            jwtService.getExpiryInSeconds());
//                    return ResponseEntity.ok(new ApiResponse<>(
//                                    "success",
//                                    "Token refreshed",
//                                    authResponse,
//                                    HttpStatus.OK.value()
//                            )
//                    );
//                })
//                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(new ApiResponse<AuthResponse>(
//                                "error",
//                                "Invalid refresh token",
//                                null,
//                                HttpStatus.UNAUTHORIZED.value())
//                        )
//                );
//    }
//


//    @PostMapping("/logout")
//    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody Map<String, String> request) {
//
//        String refreshToken = request.get("refreshToken");
//
//        if (refreshToken == null || refreshToken.isBlank()) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(new ApiResponse<Void>(
//                            "error",
//                            "Refresh token is required",
//                            null,
//                            HttpStatus.BAD_REQUEST.value()
//                    ));
//        }
//
//        return refreshTokenService.findByToken(refreshToken)
//                .map(rt -> {
//                    refreshTokenService.revoke(rt);
//                    return ResponseEntity.ok(
//                            new ApiResponse<Void>(
//                                    "success",
//                                    "Logged out successfully",
//                                    null,
//                                    HttpStatus.OK.value()
//                            )
//                    );
//                })
//                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(new ApiResponse<Void>(
//                                "error",
//                                "Invalid refresh token",
//                                null,
//                                HttpStatus.UNAUTHORIZED.value()
//                        ))
//                );
//    }


}