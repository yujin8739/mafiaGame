 package com.mafia.game.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.mafia.game.security.handler.CustomAuthFailureHandler;
import com.mafia.game.security.handler.CustomAuthSuccessHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthSuccessHandler customAuthSuccessHandler;

    SecurityConfig(CustomAuthSuccessHandler customAuthSuccessHandler) {
        this.customAuthSuccessHandler = customAuthSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form
                .loginPage("/login/view") 
                .loginProcessingUrl("/login/signin") // 실제 로그인 요청 처리 URL
                .successHandler(customAuthSuccessHandler)
                .failureHandler(new CustomAuthFailureHandler()) // 커스텀 핸들러 사용하여 메일인증 안됨 분기 처리
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/js/**", "/login/**","/images/**","/fragment/**","/chat/**","/music/**","/api/**","/godDaddy_uploadImage/**","godDaddy_uploadVideo/**","/resources/**","/introduce/**","/godDaddy_etc/**").permitAll()
                .anyRequest().authenticated()
            ).exceptionHandling(exceptionHandling -> 
            exceptionHandling
            .authenticationEntryPoint((request, response, authException) -> {
                if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                } else {
                    response.sendRedirect("/login/view");
                }
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                response.sendRedirect("/access-denied");
            }));
        
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
