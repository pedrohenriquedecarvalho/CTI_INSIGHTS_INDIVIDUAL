package com.senai.cti_insights.security;

import com.senai.cti_insights.models.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service // Responsável por assinar e validar JWTs.
public class JwtService {
    private final SecretKey chave;
    private final long expiracaoSegundos;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.expiration-minutes:480}") long expiracaoMinutos) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        // HS256 exige ao menos 256 bits (32 bytes). Segredo curto = servidor nem liga.
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoSegundos = expiracaoMinutos * 60;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail()) // Identidade incluída no token.
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoSegundos)))
                .signWith(chave, Jwts.SIG.HS256) // Assinatura HMAC SHA-256.
                .compact();
    }

    /** E-mail (subject) do token, ou vazio se o token for falso, adulterado ou vencido. */
    public Optional<String> emailDoToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpiracaoSegundos() {
        return expiracaoSegundos;
    }
}
