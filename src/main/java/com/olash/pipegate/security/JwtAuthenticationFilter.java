package com.olash.pipegate.security;


import com.olash.pipegate.common.util.JwtUtil;
import com.olash.pipegate.merchant.domain.Merchant;
import com.olash.pipegate.merchant.repository.MerchantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MerchantRepository merchantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT token received");
                filterChain.doFilter(request, response);
                return;
            }

            String merchantId = jwtUtil.extractMerchantId(token);

            if (merchantId != null &&
                    SecurityContextHolder.getContext()
                            .getAuthentication() == null) {
                Merchant merchant = merchantRepository
                        .findById(merchantId)
                        .orElse(null);

                if (merchant != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    merchant,
                                    null,
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_MERCHANT")));

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    log.info("JWT authentication successful. merchantCode={}",
                            merchant.getMerchantCode());
                }
            }

        } catch (Exception e) {
            log.error("JWT authentication failed. error={}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
