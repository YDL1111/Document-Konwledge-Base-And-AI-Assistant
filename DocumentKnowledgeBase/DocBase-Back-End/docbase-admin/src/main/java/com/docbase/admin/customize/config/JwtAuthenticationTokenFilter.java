package com.docbase.admin.customize.config;

import com.docbase.admin.customize.service.login.TokenService;
import com.docbase.infrastructure.user.AuthenticationUtils;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT token validation filter.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        SystemLoginUser loginUser = tokenService.getLoginUser(request);
        if (loginUser != null && AuthenticationUtils.getAuthentication() == null) {
            tokenService.refreshToken(loginUser);
            putCurrentLoginUserIntoContext(request, loginUser);
            log.debug("request process in jwt token filter. get login user id: {}", loginUser.getUserId());
        }
        chain.doFilter(request, response);
    }

    private void putCurrentLoginUserIntoContext(HttpServletRequest request, SystemLoginUser loginUser) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            loginUser, null, loginUser.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
