package com.gymcheck.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.gymcheck.security.jwt.JwtAuthenticationEntryPoint
import com.gymcheck.security.jwt.JwtAuthenticationFilter
import com.gymcheck.security.jwt.JwtTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun jwtAuthenticationFilter(jwtTokenProvider: JwtTokenProvider): JwtAuthenticationFilter {
        return JwtAuthenticationFilter(jwtTokenProvider)
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        objectMapper: ObjectMapper,
    ): SecurityFilterChain {
        return http
            .csrf { csrf -> csrf.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(JwtAuthenticationEntryPoint(objectMapper))
            }
            .authorizeHttpRequests { requests ->
                requests.requestMatchers(HttpMethod.GET, "/health").permitAll()
                requests.requestMatchers("/auth/oauth/**").permitAll()
                requests.requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                requests.requestMatchers(HttpMethod.DELETE, "/auth/logout").authenticated()
                requests.requestMatchers("/error").permitAll()
                requests.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
