package com.senai.cti_insights.service;

import com.senai.cti_insights.config.ApiException;
import com.senai.cti_insights.dto.*;
import com.senai.cti_insights.models.Perfil;
import com.senai.cti_insights.models.StatusUsuario;
import com.senai.cti_insights.models.Usuario;
import com.senai.cti_insights.repository.UsuarioRepository;
import com.senai.cti_insights.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service // Regras de negócio do cadastro e do login.
public class AuthService {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository repository, PasswordEncoder encoder, JwtService jwt) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Cadastro público: a conta nasce USER + PENDENTE e só entra depois que um ADMIN aprovar. */
    public MensagemResponse cadastrar(CadastroRequest dados) {
        String email = dados.email().trim().toLowerCase(Locale.ROOT);
        // BCrypt considera no máximo 72 bytes; comprimento não equivale a bytes em UTF-8.
        if (dados.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A senha é longa demais (limite do BCrypt: 72 bytes).");
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe um cadastro com este e-mail.");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(dados.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(encoder.encode(dados.senha())); // Armazena apenas o hash.
        usuario.setPerfil(Perfil.USER);                      // Cadastro público nunca define ADMIN.
        usuario.setStatus(StatusUsuario.PENDENTE);           // Espera a liberação do administrador.
        usuario.setCriadoEm(Datas.agoraUtc());
        repository.save(usuario);
        return new MensagemResponse("Cadastro recebido! Você poderá entrar assim que o administrador liberar o seu acesso.");
    }

    public LoginResponse entrar(LoginRequest dados) {
        Usuario usuario = repository.findByEmailIgnoreCase(dados.email().trim()).orElse(null);
        // Mesmo erro para usuário inexistente e senha inválida (não revela quem tem conta).
        if (usuario == null || !encoder.matches(dados.senha(), usuario.getSenhaHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
        if (usuario.getStatus() == StatusUsuario.PENDENTE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seu cadastro ainda aguarda a liberação do administrador.");
        }
        if (usuario.getStatus() == StatusUsuario.BLOQUEADO) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seu acesso está bloqueado. Fale com o administrador.");
        }
        return new LoginResponse(jwt.gerar(usuario), "Bearer", jwt.getExpiracaoSegundos(), UsuarioResponse.de(usuario));
    }
}
