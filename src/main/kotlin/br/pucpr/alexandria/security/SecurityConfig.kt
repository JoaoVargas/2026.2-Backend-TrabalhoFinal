package br.pucpr.alexandria.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { it.frameOptions { frame -> frame.disable() } }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.GET, "/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/users").permitAll()
                it.requestMatchers(
                    "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**", "/health"
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .httpBasic { }
            .exceptionHandling {
                it.authenticationEntryPoint(ProblemDetailEntryPoint())
                it.accessDeniedHandler(ProblemDetailAccessDeniedHandler())
            }

        return http.build()
    }
}
