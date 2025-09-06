package com.holo.oshi.eureka.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class EurekaSecurityConfig {

    /**
     * 개발환경용 Security Filter Chain
     * - MSA 서비스 디스커버리 최적화
     * - Basic Authentication 유지
     * - CSRF 비활성화 (RESTful API용)
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .authorizeHttpRequests { authz ->
                authz
                    .requestMatchers("/actuator/**").permitAll() 
                    .anyRequest().authenticated()                
            }
            .httpBasic { }  
            .csrf { csrf -> 
                csrf.disable() 
            }
            .build()
    }

    /**
     * 개발환경용 사용자 계정 설정
     */
    @Bean
    fun userDetailsService(): UserDetailsService {
        val user = User.builder()
            .username("eureka")
            .password("eureka_secret")
            .roles("ADMIN")
            .build()

        return InMemoryUserDetailsManager(user)
    }

    /**
     * 개발환경용 비밀번호 인코더
     * 주의: 프로덕션에서는 BCrypt 등 강력한 인코더 사용 필요
     */
    @Bean
    @Suppress("DEPRECATION")
    fun passwordEncoder(): PasswordEncoder {
        return NoOpPasswordEncoder.getInstance()
    }
}