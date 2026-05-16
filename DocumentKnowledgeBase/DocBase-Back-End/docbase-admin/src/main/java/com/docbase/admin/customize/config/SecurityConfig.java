package com.docbase.admin.customize.config;

import cn.hutool.json.JSONUtil;
import com.docbase.admin.customize.async.AsyncTaskFactory;
import com.docbase.admin.customize.service.login.LoginService;
import com.docbase.admin.customize.service.login.TokenService;
import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.enums.common.LoginStatusEnum;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode.Client;
import com.docbase.common.utils.ServletHolderUtil;
import com.docbase.domain.common.cache.RedisCacheService;
import com.docbase.infrastructure.thread.ThreadPoolManager;
import com.docbase.infrastructure.user.web.SystemLoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.filter.CorsFilter;

/**
 * Security configuration for login, logout, JWT and route authorization.
 *
 * @see UserDetailsServiceImpl#loadUserByUsername
 * @see JwtAuthenticationTokenFilter#doFilterInternal
 * @see LoginService#login
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenService tokenService;
    private final RedisCacheService redisCache;
    private final JwtAuthenticationTokenFilter jwtTokenFilter;
    private final UserDetailsService userDetailsService;
    private final CorsFilter corsFilter;

    @Bean
    public AuthenticationEntryPoint unauthorizedHandler() {
        return (request, response, exception) -> {
            ResponseDTO<Object> responseDTO = ResponseDTO.fail(
                new ApiException(Client.COMMON_NO_AUTHORIZATION, request.getRequestURI())
            );
            ServletHolderUtil.renderString(response, JSONUtil.toJsonStr(responseDTO));
        };
    }

    @Bean
    public LogoutSuccessHandler logOutSuccessHandler() {
        return (request, response, authentication) -> {
            SystemLoginUser loginUser = tokenService.getLoginUser(request);
            if (loginUser != null) {
                String userName = loginUser.getUsername();
                redisCache.loginUserCache.delete(loginUser.getCachedKey());
                ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(
                    userName, LoginStatusEnum.LOGOUT, LoginStatusEnum.LOGOUT.description()));
            }
            ServletHolderUtil.renderString(response, JSONUtil.toJsonStr(ResponseDTO.ok()));
        };
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
            .userDetailsService(userDetailsService)
            .passwordEncoder(bCryptPasswordEncoder())
            .and()
            .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/getConfig", "/captchaImage", "/api/**").anonymous()
                .requestMatchers(HttpMethod.GET, "/", "/*.html", "/**/*.html", "/**/*.css", "/**/*.js",
                    "/profile/**").permitAll()
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/*/api-docs",
                    "/*/api-docs/swagger-config",
                    "/**/api-docs.yaml",
                    "/v3/api-docs/**",
                    "/druid/**"
                ).anonymous()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        httpSecurity.logout().logoutUrl("/logout").logoutSuccessHandler(logOutSuccessHandler());
        httpSecurity.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class);
        httpSecurity.addFilterBefore(corsFilter, LogoutFilter.class);

        return httpSecurity.build();
    }
}
