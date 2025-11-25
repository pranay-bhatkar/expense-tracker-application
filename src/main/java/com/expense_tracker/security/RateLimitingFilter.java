package com.expense_tracker.security;


import org.springframework.stereotype.Component;

@Component
public class RateLimitingFilter {

//public class RateLimitingFilter extends OncePerRequestFilter {

//    // Map to hold user-specific buckets (or IP-specific)
//    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
//
//    private Bucket resolveBucket(String key) {
//        return cache.computeIfAbsent(key, k -> {
//            Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
//            Bucket bucket = Bucket.builder().addLimit(limit).build();
//            return bucket;
//        });
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        String key = request.getRemoteAddr(); // rate limit per IP (you can also use user ID)
//
//        Bucket bucket = resolveBucket(key);
//
//        if (bucket.tryConsume(1)) {
//            filterChain.doFilter(request, response);
//        } else {
//            response.setStatus(429); // Too Many Requests
//            response.getWriter().write("Too many requests - try again later.");
//        }
//    }
}