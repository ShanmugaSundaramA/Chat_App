package com.ws.chat.securityconfig;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ws.chat.jwt.AuthEntryPointJwt;
import com.ws.chat.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

     private final JwtAuthenticationFilter jwtAuthenticationFilter;
     private final AuthenticationProvider authenticationProvider;
     private final AuthEntryPointJwt unauthorizedHandler;

     @Bean
     SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

          http.csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                    .authorizeHttpRequests(requests -> requests
                              .requestMatchers("/findUserByEmail" , "/**")
                              .permitAll()
                              .requestMatchers("/api/swagger-ui/**",
                                        "/swagger-ui/**",
                                        "/swagger-resources/*",
                                        "/v3/api-docs/**")
                              .permitAll()
                              .requestMatchers("/actuator", "/actuator/**")
                              .permitAll()
                              .anyRequest().authenticated());
          http.authenticationProvider(authenticationProvider);
          http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
          http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
          return http.build();
     }

     @Bean
     CorsConfigurationSource corsConfigurationSource() {

          CorsConfiguration configuration = new CorsConfiguration();
          configuration.setAllowedOrigins(Arrays.asList("*"));
          configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
          configuration.setAllowedHeaders(List.of("*"));
          UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", configuration);
          return source;
     }

}
