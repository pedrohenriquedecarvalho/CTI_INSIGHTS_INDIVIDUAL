package com.senai.cti_insights.security;

import com.senai.cti_insights.models.StatusUsuario;
import com.senai.cti_insights.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê "Authorization: Bearer <token>", valida e autentica a requisição.
 * NÃO é um @Component de propósito: o SecurityConfig cria uma única instância dentro da cadeia do
 * Spring Security (como @Component, o Spring Boot registraria o filtro uma segunda vez).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UsuarioRepository repository;

    public JwtAuthenticationFilter(JwtService jwt, UsuarioRepository repository) {
        this.jwt = jwt;
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecalho.substring(7).trim();
            jwt.emailDoToken(token)                                  // verifica assinatura e expiração
                    .flatMap(repository::findByEmailIgnoreCase)
                    // Se o admin bloquear a conta, o token antigo deixa de valer na hora.
                    .filter(u -> u.getStatus() == StatusUsuario.APROVADO)
                    .ifPresent(u -> {
                        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name()));
                        var autenticacao = new UsernamePasswordAuthenticationToken(u.getEmail(), null, autoridades);
                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    });
            // Token inválido: não autentica; o SecurityConfig devolve 401 nas rotas privadas.
        }
        chain.doFilter(request, response);
    }
}
