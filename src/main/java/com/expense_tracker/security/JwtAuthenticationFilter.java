package com.expense_tracker.security;

import com.expense_tracker.model.RefreshToken;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.RefreshTokenRepository;
import com.expense_tracker.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // if no header, continue filter chain (will be blocked by Spring Security if required)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username = jwtService.extractUserName(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 1️⃣ Validate access token
            if (!jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2️⃣ Load User entity (important!)
            User userEntity = userRepository.findByEmail(username).orElse(null);

            if (userEntity != null) {
                Optional<RefreshToken> refreshToken =
                        refreshTokenRepository.findTopByUserOrderByIdDesc(userEntity);

                // 3️⃣ If refresh token REVOKED → logout should block all future requests
                if (refreshToken.isPresent() && refreshToken.get().isRevoked()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token revoked. Please login again.");
                    return;
                }
            }

            // 4️⃣ Token valid → authenticate user
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

}