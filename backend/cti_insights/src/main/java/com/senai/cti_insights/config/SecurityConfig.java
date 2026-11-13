package com.senai.cti_insights.config;

import com.senai.cti_insights.repository.UsuarioRepository;
import com.senai.cti_insights.security.JwtAuthenticationFilter;
import com.senai.cti_insights.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10); // Hash de senhas; nunca criptografia reversível.
    }

    @Bean
    public SecurityFilterChain filtro(HttpSecurity http, JwtService jwt, UsuarioRepository usuarios) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // API stateless com Bearer no header.
            .cors(Customizer.withDefaults()) // usa o corsConfigurationSource abaixo
            .formLogin(login -> login.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, erro) -> escreverErro(res, 401, "Faça login para continuar."))
                .accessDeniedHandler((req, res, erro) -> escreverErro(res, 403, "Você não tem permissão para isso.")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // o navegador pergunta antes (CORS)
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health", "/api/publico").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN") // só ADMIN aprova/bloqueia usuários
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwt, usuarios), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /** CORS: sem isto o navegador bloqueia o front (porta 5173) de chamar a API (porta 8080). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins}") String origens) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(origens.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", cfg);
        return fonte;
    }

    private static void escreverErro(HttpServletResponse res, int status, String mensagem) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
