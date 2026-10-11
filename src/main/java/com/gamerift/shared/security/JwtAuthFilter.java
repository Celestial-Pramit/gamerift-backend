package com.gamerift.shared.security;

import java.io.IOException;
import java.util.List;

import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs once for EVERY request, before the controller.
 *
 * Steps: 1. read the header "Authorization: Bearer <token>"
 *        2. check the token with JwtUtil
 *        3. load the user from the database (so a ban or role change works immediately)
 *        4. if all is fine, tell Spring Security who the user is (SecurityContext).
 * If anything is wrong we simply do NOT authenticate; the security rules then answer 401.
 *
 * It is created with "new" inside SecurityConfig (not as a @Component) so it runs only in the security chain.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String userId = jwtUtil.validateAndGetUserId(header.substring(7));
            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && !user.isBanned()) {
                    AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getRole());
                    // Spring expects role names with the prefix ROLE_
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
